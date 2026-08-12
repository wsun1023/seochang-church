package com.seochang.church.repository;

import com.seochang.church.entity.Board;
import com.seochang.church.entity.BoardLike;
import com.seochang.church.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {
    Optional<BoardLike> findByBoardAndUser(Board board, User user);
    boolean existsByBoardAndUser(Board board, User user);
    long countByBoard(Board board);
}
