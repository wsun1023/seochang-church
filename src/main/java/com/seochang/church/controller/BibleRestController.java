package com.seochang.church.controller;

import com.seochang.church.entity.User;
import com.seochang.church.service.BibleProgressService;
import com.seochang.church.service.BibleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class BibleRestController {

    @Autowired
    private BibleService bibleService;

    @Autowired
    private BibleProgressService bibleProgressService;

    @GetMapping("/api/bible")
    public List<Map<String, String>> getBibleChapter(
            @RequestParam(defaultValue = "1") int testament,
            @RequestParam(defaultValue = "1") int book,
            @RequestParam(defaultValue = "1") int chapter,
            @RequestParam(defaultValue = "catholic") String version) {
        return bibleService.getBibleChapter(testament, book, chapter, version);
    }

    @GetMapping("/api/bible/progress")
    public ResponseEntity<Map<String, Object>> getProgress(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        Long userId = loginUser != null ? loginUser.getId() : null;
        return ResponseEntity.ok(bibleProgressService.getProgressSummary(userId));
    }

    @PostMapping("/api/bible/progress/toggle")
    public ResponseEntity<Map<String, Object>> toggleChapter(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        try {
            int testament = toInt(payload.get("testament"));
            int book = toInt(payload.get("book"));
            int chapter = toInt(payload.get("chapter"));
            Boolean desiredState = payload.get("read") instanceof Boolean b ? b : null;

            Map<String, Object> result = bibleProgressService.toggleChapter(
                    loginUser.getId(), testament, book, chapter, desiredState);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/bible/progress/book")
    public ResponseEntity<Map<String, Object>> setBookProgress(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        try {
            int testament = toInt(payload.get("testament"));
            int book = toInt(payload.get("book"));
            boolean readAll = Boolean.TRUE.equals(payload.get("read"));

            Map<String, Object> result = bibleProgressService.setBookProgress(
                    loginUser.getId(), testament, book, readAll);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private int toInt(Object val) {
        if (val instanceof Number n) {
            return n.intValue();
        }
        if (val instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        throw new IllegalArgumentException("올바르지 않은 요청 값입니다.");
    }
}
