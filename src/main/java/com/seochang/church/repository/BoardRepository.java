package com.seochang.church.repository;

import com.seochang.church.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByDelYnOrderByCreatedAtDesc(String delYn);
    List<Board> findByDelYnAndCategoryOrderByCreatedAtDesc(String delYn, String category);

    Page<Board> findByDelYn(String delYn, Pageable pageable);
    Page<Board> findByDelYnAndCategory(String delYn, String category, Pageable pageable);
    
    @Query("SELECT b FROM Board b WHERE b.delYn = 'N' AND b.category = :category AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.writer) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Board> findActiveByCategoryAndKeyword(@Param("category") String category, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT b FROM Board b WHERE b.delYn = 'N' AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.writer) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Board> findActiveByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT b FROM Board b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.writer) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Board> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    long countByDelYn(String delYn);

    long countByDelYnAndCategory(String delYn, String category);
}
