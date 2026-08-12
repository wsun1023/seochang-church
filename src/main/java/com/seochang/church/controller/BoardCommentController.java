package com.seochang.church.controller;

import com.seochang.church.dto.BoardCommentRequest;
import com.seochang.church.entity.Board;
import com.seochang.church.entity.BoardComment;
import com.seochang.church.entity.User;
import com.seochang.church.repository.BoardCommentRepository;
import com.seochang.church.repository.BoardRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/boards")
public class BoardCommentController {

    private final BoardRepository boardRepository;
    private final BoardCommentRepository boardCommentRepository;

    public BoardCommentController(BoardRepository boardRepository, BoardCommentRepository boardCommentRepository) {
        this.boardRepository = boardRepository;
        this.boardCommentRepository = boardCommentRepository;
    }

    @PostMapping("/{boardId}/comments")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable Long boardId,
            @RequestBody BoardCommentRequest request,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        User loginUser = (User) session.getAttribute("loginUser");
        
        if (loginUser == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(response);
        }

        Optional<Board> boardOpt = boardRepository.findById(boardId);
        if (boardOpt.isEmpty() || "Y".equals(boardOpt.get().getDelYn())) {
            response.put("success", false);
            response.put("message", "게시글을 찾을 수 없습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        Board board = boardOpt.get();
        String secretYn = request.isSecret() ? "Y" : "N";
        String writerName = loginUser.getBaptismalName() != null && !loginUser.getBaptismalName().isEmpty() 
                ? loginUser.getName() + " (" + loginUser.getBaptismalName() + ")" 
                : loginUser.getName();

        BoardComment comment = new BoardComment(board, request.getContent(), writerName, loginUser.getId(), secretYn);
        if (request.getParentId() != null) {
            comment.setParentId(request.getParentId());
        }
        boardCommentRepository.save(comment);

        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{boardId}/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long boardId,
            @PathVariable Long commentId,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        User loginUser = (User) session.getAttribute("loginUser");
        
        if (loginUser == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(response);
        }

        Optional<BoardComment> commentOpt = boardCommentRepository.findById(commentId);
        if (commentOpt.isEmpty() || !commentOpt.get().getBoard().getId().equals(boardId)) {
            response.put("success", false);
            response.put("message", "댓글을 찾을 수 없습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        BoardComment comment = commentOpt.get();
        
        // 권한 확인: 본인 또는 ADMIN
        if (!comment.getWriterId().equals(loginUser.getId()) && !"ADMIN".equals(loginUser.getRole())) {
            response.put("success", false);
            response.put("message", "삭제 권한이 없습니다.");
            return ResponseEntity.status(403).body(response);
        }

        comment.setDelYn("Y");
        boardCommentRepository.save(comment);

        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}
