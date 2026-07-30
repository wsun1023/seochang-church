package com.seochang.church.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BibleService {

    // Simple memory cache: Key is "m_n_p", Value is list of verses
    private final Map<String, List<Map<String, String>>> cache = new ConcurrentHashMap<>();

    public List<Map<String, String>> getBibleChapter(int testament, int book, int chapter) {
        String cacheKey = testament + "_" + book + "_" + chapter;
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        List<Map<String, String>> verses = new ArrayList<>();

        int mappedBook = testament == 2 ? book + 46 : book;
        // m=1 is 가톨릭 성경 (Standard Catholic Bible). n is book index 1~73. p is chapter.
        String url = String.format("https://maria.catholic.or.kr/bible/read/bible_read.asp?m=%d&n=%d&p=%d", testament, mappedBook, chapter);

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .sslSocketFactory(socketFactory())
                    .get();

            // Usually, verses in GoodNews are in <ul class="b_list"> <li>...
            Elements rows = doc.select("tbody tr");
            for (Element row : rows) {
                Elements numCols = row.select("td.num_color");
                Elements textCols = row.select("td.al, td.tt");
                if (!textCols.isEmpty()) {
                    String num = numCols.isEmpty() ? "" : numCols.text().trim();
                    String text = textCols.first().text().trim();
                    if (!text.isEmpty()) {
                        Map<String, String> verseInfo = new HashMap<>();
                        verseInfo.put("verse", num);
                        verseInfo.put("text", text);
                        verses.add(verseInfo);
                    }
                }
            }

            if (!verses.isEmpty()) {
                cache.put(cacheKey, verses);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return verses;
    }

    private javax.net.ssl.SSLSocketFactory socketFactory() {
        try {
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                }
            };
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSL socket factory", e);
        }
    }
}
