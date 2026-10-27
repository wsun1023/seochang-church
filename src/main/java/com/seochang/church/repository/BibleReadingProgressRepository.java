package com.seochang.church.repository;

import com.seochang.church.entity.BibleReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BibleReadingProgressRepository extends JpaRepository<BibleReadingProgress, Long> {

    List<BibleReadingProgress> findByUser_IdOrderByTestamentAscBookAscChapterAsc(Long userId);

    Optional<BibleReadingProgress> findByUser_IdAndTestamentAndBookAndChapter(Long userId, int testament, int book, int chapter);

    List<BibleReadingProgress> findByUser_IdAndTestamentAndBook(Long userId, int testament, int book);

    void deleteByUser_IdAndTestamentAndBook(Long userId, int testament, int book);
}
