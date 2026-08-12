package com.seochang.church.controller;

import com.seochang.church.entity.Board;
import com.seochang.church.entity.BoardLike;
import com.seochang.church.entity.User;
import com.seochang.church.repository.BoardLikeRepository;
import com.seochang.church.repository.BoardRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/boards")
public class BoardLikeController {

    private final BoardRepository boardRepository;
    private final BoardLikeRepository boardLikeRepository;

    public BoardLikeController(BoardRepository boardRepository, BoardLikeRepository boardLikeRepository) {
        this.boardRepository = boardRepository;
        this.boardLikeRepository = boardLikeRepository;
    }

    @PostMapping("/{boardId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long boardId, HttpSession session) {
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
        Optional<BoardLike> likeOpt = boardLikeRepository.findByBoardAndUser(board, loginUser);

        boolean isLiked;
        if (likeOpt.isPresent()) {
            boardLikeRepository.delete(likeOpt.get());
            board.setLikeCount(Math.max(0, board.getLikeCount() - 1));
            isLiked = false;
        } else {
            BoardLike boardLike = new BoardLike(board, loginUser);
            boardLikeRepository.save(boardLike);
            board.setLikeCount(board.getLikeCount() + 1);
            isLiked = true;
        }
        
        boardRepository.save(board);

        response.put("success", true);
        response.put("isLiked", isLiked);
        response.put("likeCount", board.getLikeCount());
        
        return ResponseEntity.ok(response);
    }
}
