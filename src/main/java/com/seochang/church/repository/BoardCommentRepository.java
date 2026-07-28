package com.seochang.church.repository;

import com.seochang.church.entity.BoardComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {
    List<BoardComment> findByBoardIdAndDelYnOrderByCreatedAtAsc(Long boardId, String delYn);
}
