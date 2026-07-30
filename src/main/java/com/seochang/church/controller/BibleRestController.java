package com.seochang.church.controller;

import com.seochang.church.service.BibleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class BibleRestController {

    @Autowired
    private BibleService bibleService;

    @GetMapping("/api/bible")
    public List<Map<String, String>> getBibleChapter(
            @RequestParam(defaultValue = "1") int testament,
            @RequestParam(defaultValue = "1") int book,
            @RequestParam(defaultValue = "1") int chapter) {
        return bibleService.getBibleChapter(testament, book, chapter);
    }
}
