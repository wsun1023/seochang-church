package com.seochang.church.service;

import com.seochang.church.dto.DailyMissaDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.util.StringUtils;

@Service
public class DailyMissaService {

    private static final String DAILY_MISSA_URL = "https://maria.catholic.or.kr/mi_pr/missa/missa.asp";

    public DailyMissaDto getDailyMissa(String dateStr) {
        DailyMissaDto dto = new DailyMissaDto();
        
        LocalDate targetDate;
        if (StringUtils.hasText(dateStr)) {
            try {
                targetDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                targetDate = LocalDate.now();
            }
        } else {
            targetDate = LocalDate.now();
        }

        dto.setDate(targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        dto.setPrevDate(targetDate.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        dto.setNextDate(targetDate.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        String requestUrl = DAILY_MISSA_URL + "?goMonth=" + dto.getDate();

        try {
            Document doc = Jsoup.connect(requestUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .sslSocketFactory(socketFactory())
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
                    dto.addReading(secName, contentBuilder.toString());
                }
            }

            if (dto.getReadings().isEmpty()) {
                dto.addReading("안내", "<p class='text-muted'>오늘은 매일미사 정보가 없거나, 굿뉴스 서버의 구조가 변경되었습니다.</p>");
            }

        } catch (IOException e) {
            e.printStackTrace();
            dto.setTitle("매일미사 정보를 가져올 수 없습니다.");
            dto.addReading("오류", "<p class='text-danger'>가톨릭 굿뉴스 서버와 연결할 수 없습니다. 나중에 다시 시도해 주세요.</p>");
        }

        return dto;
    }

    private SSLSocketFactory socketFactory() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }};

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create a SSL socket factory", e);
        }
    }
}
