package com.seochang.church.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.time.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DailyMissaCacheTests {
    private final CatholicHttpClient client = mock(CatholicHttpClient.class);
    private final Connection connection = mock(Connection.class);
    private final Clock clock = mock(Clock.class);

    private DailyMissaService service() throws Exception {
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(clock.instant()).thenReturn(Instant.parse("2026-09-09T14:59:00Z"));
        when(client.connect(anyString(), anyInt())).thenReturn(connection);
        when(connection.get()).thenReturn(Jsoup.parse("<title>매일미사</title><div class='bd_tit'>복음</div><p>오늘의 말씀</p>"));
        return new DailyMissaService(client, clock);
    }

    @Test void simultaneousRequestsScrapeOnceAndCopiesProtectCache() throws Exception {
        var service = service();
        var pool = Executors.newFixedThreadPool(12);
        var start = new CountDownLatch(1);
        try {
            var results = new ArrayList<Future<String>>();
            for (int i = 0; i < 24; i++) results.add(pool.submit(() -> {
                start.await();
                return service.getDailyMissa(null).getDate();
            }));
            start.countDown();
            for (var result : results) assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("2026-09-09");
            var changed = service.getDailyMissa("2026-09-09");
            changed.getReadings().get(0).setContent("changed");
            changed.getReadings().clear();
            assertThat(service.getDailyMissa("invalid").getReadings().get(0).getContent()).contains("오늘의 말씀");
            verify(connection, times(1)).get();
        } finally { pool.shutdownNow(); }
    }

    @Test void koreanMidnightRefreshesCacheAndDatesStaySeparate() throws Exception {
        var service = service();
        service.getDailyMissa(null);
        service.getDailyMissa("2026-09-08");
        service.getDailyMissa("2026-09-08");
        verify(connection, times(2)).get();
        when(clock.instant()).thenReturn(Instant.parse("2026-09-09T15:00:00Z"));
        assertThat(service.getDailyMissa(null).getDate()).isEqualTo("2026-09-10");
        service.getDailyMissa("2026-09-09");
        verify(connection, times(4)).get();
    }

    @Test void failuresAndEmptyPagesAreRetried() throws Exception {
        var service = service();
        when(connection.get()).thenThrow(new IOException("offline"))
                .thenReturn(Jsoup.parse("<title>empty</title>"))
                .thenReturn(Jsoup.parse("<div class='bd_tit'>복음</div><p>회복</p>"));
        assertThat(service.getDailyMissa(null).getReadings().get(0).getType()).isEqualTo("오류");
        assertThat(service.getDailyMissa(null).getReadings().get(0).getType()).isEqualTo("안내");
        assertThat(service.getDailyMissa(null).getReadings().get(0).getContent()).contains("회복");
        service.getDailyMissa(null);
        verify(connection, times(3)).get();
    }

    @Test void manySelectedDatesDoNotEvictToday() throws Exception {
        var service = service();
        service.getDailyMissa(null);
        for (int i = 1; i <= 100; i++) service.getDailyMissa(LocalDate.of(2026, 9, 9).minusDays(i).toString());
        service.getDailyMissa(null);
        verify(connection, times(101)).get();
    }

    @Test void reportWarmCacheLatencyWithoutNetwork() throws Exception {
        var service = service();
        service.getDailyMissa(null);
        for (int i = 0; i < 1000; i++) service.getDailyMissa(null);
        long[] samples = new long[10000];
        for (int i = 0; i < samples.length; i++) {
            long start = System.nanoTime();
            service.getDailyMissa(null);
            samples[i] = System.nanoTime() - start;
        }
        java.util.Arrays.sort(samples);
        System.out.printf("Daily Mass warm cache: p50=%.3f ms, p95=%.3f ms, p99=%.3f ms%n",
                samples[5000] / 1e6, samples[9500] / 1e6, samples[9900] / 1e6);
        verify(connection, times(1)).get();
    }
}
