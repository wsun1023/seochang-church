package com.seochang.church.service;

import com.seochang.church.dto.DailyMissaDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Clock;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;
import java.time.format.DateTimeFormatter;
import org.springframework.util.StringUtils;

@Service
public class DailyMissaService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DailyMissaService.class);
    private final CatholicHttpClient httpClient;
    private final Clock clock;
    private volatile DailyCache cache;

    private static final class DailyCache {
        final LocalDate day;
        final ConcurrentHashMap<LocalDate, DailyMissaDto> entries = new ConcurrentHashMap<>();
        DailyCache(LocalDate day) { this.day = day; }
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DailyMissaService(CatholicHttpClient httpClient) {
        this(httpClient, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    DailyMissaService(CatholicHttpClient httpClient, Clock clock) {
        this.httpClient = httpClient;
        this.clock = clock;
    }

    private static final String DAILY_MISSA_URL = "https://maria.catholic.or.kr/mi_pr/missa/missa.asp";

    public DailyMissaDto getDailyMissa(String dateStr) {
        LocalDate today = LocalDate.now(clock);
        LocalDate targetDate;
        if (StringUtils.hasText(dateStr)) {
            try {
                targetDate = LocalDate.parse(dateStr);
            } catch (Exception e) {
                targetDate = today;
            }
        } else {
            targetDate = today;
        }

        DailyCache current = cacheFor(today);
        DailyMissaDto hit = current.entries.get(targetDate);
        if (hit != null) return copy(hit);
        synchronized (current) {
            hit = current.entries.get(targetDate);
            if (hit != null) return copy(hit);
            DailyMissaDto loaded = scrape(targetDate);
            // Error/empty responses must be retried on the next visit.
            if (!loaded.getReadings().isEmpty()
                    && !loaded.getReadings().get(0).getType().equals("오류")
                    && !loaded.getReadings().get(0).getType().equals("안내")) {
                if (current.entries.size() >= 64) {
                    current.entries.keySet().stream().filter(date -> !date.equals(today))
                            .findFirst().ifPresent(current.entries::remove);
                }
                current.entries.put(targetDate, loaded);
            }
            return copy(loaded);
        }
    }

    private DailyCache cacheFor(LocalDate today) {
        DailyCache current = cache;
        if (current != null && current.day.equals(today)) return current;
        synchronized (this) {
            if (cache == null || !cache.day.equals(today)) cache = new DailyCache(today);
            return cache;
        }
    }

    private static DailyMissaDto copy(DailyMissaDto source) {
        DailyMissaDto result = new DailyMissaDto();
        result.setDate(source.getDate());
        result.setPrevDate(source.getPrevDate());
        result.setNextDate(source.getNextDate());
        result.setTitle(source.getTitle());
        result.setDateText(source.getDateText());
        result.setLiturgicalDay(source.getLiturgicalDay());
        source.getReadings().forEach(reading -> result.addReading(reading.getType(), reading.getContent()));
        return result;
    }

    private DailyMissaDto scrape(LocalDate targetDate) {
        DailyMissaDto dto = new DailyMissaDto();

        dto.setDate(targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        dto.setPrevDate(targetDate.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        dto.setNextDate(targetDate.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        String requestUrl = DAILY_MISSA_URL + "?goMonth=" + dto.getDate();

        try {
            Document doc = httpClient.connect(requestUrl, 10000)
                    .get();

            // Extract date text
            Element todayElem = doc.selectFirst(".today");
            if (todayElem != null) {
                dto.setDateText(todayElem.text().trim());
            }

            // Extract liturgical info
            Element liturgicalElem = doc.selectFirst(".m_t_today01");
            if (liturgicalElem != null) {
                dto.setLiturgicalDay(liturgicalElem.text().trim());
            }

            // Try to find the title, usually it has date info
            Element titleElement = doc.selectFirst("title");
            if (titleElement != null) {
                String fullTitle = titleElement.text();
                // Usually "가톨릭 굿뉴스 매일미사 - [날짜] [축일]"
                String cleanedTitle = fullTitle.replace("가톨릭 인터넷 굿뉴스", "").replace("가톨릭 굿뉴스", "").replace("매일미사", "").replace("-", "").trim();
                dto.setTitle(cleanedTitle.isEmpty() ? "오늘의 매일미사" : "매일미사: " + cleanedTitle);
            } else {
                dto.setTitle("오늘의 매일미사");
            }

            // The main content area
            Elements sections = doc.select(".bd_tit");
            for (Element sectionTitle : sections) {
                String secName = sectionTitle.text().trim();
                
                // 사용자의 요청에 따라 '오늘의 묵상'과 '오늘의 강론' 섹션은 크롤링에서 제외
                if (secName.contains("오늘의 묵상") || secName.contains("오늘의 강론") || secName.contains("파견")) {
                    continue;
                }

                StringBuilder contentBuilder = new StringBuilder();
                Element sibling = sectionTitle.nextElementSibling();
                
                // Read siblings until the next .bd_tit or the end of the container
                while (sibling != null && !sibling.hasClass("bd_tit")) {
                    // Avoid appending empty or script/style tags if any, but generally appending is fine
                    if (!sibling.tagName().equals("script") && !sibling.tagName().equals("style")) {
                        contentBuilder.append(sibling.outerHtml());
                    }
                    sibling = sibling.nextElementSibling();
                }
                
                if (contentBuilder.length() > 0) {
                    dto.addReading(secName, Jsoup.clean(contentBuilder.toString(), org.jsoup.safety.Safelist.relaxed()));
                }
            }

            if (dto.getReadings().isEmpty()) {
                dto.addReading("안내", "<p class='text-muted'>오늘은 매일미사 정보가 없거나, 굿뉴스 서버의 구조가 변경되었습니다.</p>");
            }

        } catch (IOException e) {
            log.warn("Daily Mass request failed for {}: {}", dto.getDate(), e.toString());
            dto.setTitle("매일미사 정보를 가져올 수 없습니다.");
            dto.addReading("오류", "<p class='text-danger'>가톨릭 굿뉴스 서버와 연결할 수 없습니다. 나중에 다시 시도해 주세요.</p>");
        }

        return dto;
    }

}
