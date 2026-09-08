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
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BibleService.class);
    private final CatholicHttpClient httpClient;

    public BibleService(CatholicHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    // Simple memory cache: Key is "version_testament_book_chapter", Value is list of verses
    private final Map<String, List<Map<String, String>>> cache = new ConcurrentHashMap<>();

    public List<Map<String, String>> getBibleChapter(int testament, int book, int chapter) {
        return getBibleChapter(testament, book, chapter, "catholic");
    }

    public List<Map<String, String>> getBibleChapter(int testament, int book, int chapter, String version) {
        String normalizedVersion = (version != null && version.equalsIgnoreCase("joint")) ? "joint" : "catholic";
        String cacheKey = normalizedVersion + "_" + testament + "_" + book + "_" + chapter;
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        String url = buildBibleUrl(testament, book, chapter, normalizedVersion);
        List<Map<String, String>> verses = new ArrayList<>();

        try {
            Document doc = httpClient.connect(url, 5000)
                    .get();
            verses = parseVerses(doc);

            if (!verses.isEmpty()) {
                cache.put(cacheKey, verses);
            }
        } catch (IOException e) {
            log.warn("Bible request failed for chapter {}: {}", cacheKey, e.toString());
        }

        return verses;
    }

    public String buildBibleUrl(int testament, int book, int chapter, String version) {
        String normalizedVersion = (version != null && version.equalsIgnoreCase("joint")) ? "joint" : "catholic";
        int m = testament;
        int n;
        if ("joint".equals(normalizedVersion)) {
            // 공동번역 성서: 구약 1~46, 신약 47~73
            n = testament == 2 ? book + 46 : book;
        } else {
            // 한국 천주교회 공용 번역본 (주교회의 성경): 구약 101~146, 신약 147~173
            n = testament == 2 ? book + 146 : book + 100;
        }
        return String.format("https://maria.catholic.or.kr/bible/read/bible_read.asp?m=%d&n=%d&p=%d", m, n, chapter);
    }

    List<Map<String, String>> parseVerses(Document doc) {
        List<Map<String, String>> verses = new ArrayList<>();
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
        return verses;
    }
}
