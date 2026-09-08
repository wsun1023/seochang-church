package com.seochang.church.controller;

import com.seochang.church.entity.User;
import com.seochang.church.service.BoardLikeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/boards")
public class BoardLikeController {
    private final BoardLikeService likes;
    public BoardLikeController(BoardLikeService likes) { this.likes = likes; }
    @PostMapping("/{boardId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long boardId, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        var result = likes.toggle(boardId, user);
        return ResponseEntity.ok(Map.of("success", true, "isLiked", result.isLiked(), "likeCount", result.likeCount()));
    }
}
