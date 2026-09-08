package com.seochang.church.service;

import com.seochang.church.entity.*;
import com.seochang.church.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BoardLikeService {
    private final BoardRepository boards;
    private final BoardLikeRepository likes;
    public BoardLikeService(BoardRepository boards, BoardLikeRepository likes) {
        this.boards = boards;
        this.likes = likes;
    }
    public record Result(boolean isLiked, int likeCount) {}
    @Transactional
    public Result toggle(Long boardId, User user) {
        Board board = boards.findLockedById(boardId)
                .filter(b -> "N".equals(b.getDelYn()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        var previous = likes.findByBoardAndUser(board, user);
        boolean liked = previous.isEmpty();
        if (liked) likes.save(new BoardLike(board, user));
        else likes.delete(previous.get());
        likes.flush();
        int count = Math.toIntExact(likes.countByBoard(board));
        board.setLikeCount(count);
        return new Result(liked, count);
    }
}
