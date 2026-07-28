package com.seochang.church.repository;

import com.seochang.church.entity.Gallery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GalleryRepository extends JpaRepository<Gallery, Long> {
    Page<Gallery> findByDelYn(String delYn, Pageable pageable);
    Page<Gallery> findByDelYnAndTitleContaining(String delYn, String keyword, Pageable pageable);
    
    @Query("SELECT g FROM Gallery g WHERE g.delYn = 'N' ORDER BY g.createdAt DESC")
    List<Gallery> findTop3ByOrderByCreatedAtDesc(Pageable pageable);
}
