package com.seochang.church.repository;

import com.seochang.church.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    // Find all active banners/popups that are within their valid date range
    @Query("SELECT b FROM Banner b WHERE b.isActive = true AND (b.startDate IS NULL OR b.startDate <= :today) AND (b.endDate IS NULL OR b.endDate >= :today) ORDER BY b.createdAt DESC")
    List<Banner> findActiveBanners(LocalDate today);
    
    // Specifically find by type (POPUP or BANNER)
    @Query("SELECT b FROM Banner b WHERE b.isActive = true AND b.type = :type AND (b.startDate IS NULL OR b.startDate <= :today) AND (b.endDate IS NULL OR b.endDate >= :today) ORDER BY b.createdAt DESC")
    List<Banner> findActiveBannersByType(String type, LocalDate today);
    
    // Admin list sorted by creation
    List<Banner> findAllByOrderByCreatedAtDesc();
}
