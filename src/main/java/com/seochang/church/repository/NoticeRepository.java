package com.seochang.church.repository;

import com.seochang.church.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByCategoryOrderByCreatedAtDesc(String category);
    
    Page<Notice> findByCategory(String category, Pageable pageable);
    
    @Query("SELECT n FROM Notice n WHERE n.category = :category AND LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Notice> findByCategoryAndTitleKeyword(@Param("category") String category, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT n FROM Notice n WHERE LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Notice> findByTitleKeyword(@Param("keyword") String keyword, Pageable pageable);
}