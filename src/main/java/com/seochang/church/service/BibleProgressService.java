package com.seochang.church.service;

import com.seochang.church.entity.BibleReadingProgress;
import com.seochang.church.entity.User;
import com.seochang.church.repository.BibleReadingProgressRepository;
import com.seochang.church.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BibleProgressService {

    public static final int[] OLD_TESTAMENT_CHAPTERS = {
            50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 14, 16, 10, 16, 15,
            42, 150, 31, 12, 8, 19, 51, 66, 52, 5, 6, 48, 14, 14, 4, 9, 1, 4, 7, 3, 3, 3, 2, 14, 3
    };

    public static final int[] NEW_TESTAMENT_CHAPTERS = {
            28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1, 1, 1, 22
    };

    public static final int OLD_TESTAMENT_BOOKS = OLD_TESTAMENT_CHAPTERS.length; // 46
    public static final int NEW_TESTAMENT_BOOKS = NEW_TESTAMENT_CHAPTERS.length; // 27
    public static final int TOTAL_BOOKS = OLD_TESTAMENT_BOOKS + NEW_TESTAMENT_BOOKS; // 73

    public static final int OLD_TESTAMENT_TOTAL_CHAPTERS = Arrays.stream(OLD_TESTAMENT_CHAPTERS).sum(); // 1074
    public static final int NEW_TESTAMENT_TOTAL_CHAPTERS = Arrays.stream(NEW_TESTAMENT_CHAPTERS).sum(); // 260
    public static final int TOTAL_CHAPTERS = OLD_TESTAMENT_TOTAL_CHAPTERS + NEW_TESTAMENT_TOTAL_CHAPTERS; // 1334

    private final BibleReadingProgressRepository progressRepository;
    private final UserRepository userRepository;

    public BibleProgressService(BibleReadingProgressRepository progressRepository, UserRepository userRepository) {
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
    }

    public void validateChapter(int testament, int book, int chapter) {
        int maxChapter = getMaxChapters(testament, book);
        if (chapter < 1 || chapter > maxChapter) {
            throw new IllegalArgumentException("유효하지 않은 성경 장 번호입니다: " + chapter);
        }
    }

    public int getMaxChapters(int testament, int book) {
        if (testament == 1) {
            if (book < 1 || book > OLD_TESTAMENT_BOOKS) {
                throw new IllegalArgumentException("구약성경 권 번호는 1~46 사이여야 합니다.");
            }
            return OLD_TESTAMENT_CHAPTERS[book - 1];
        } else if (testament == 2) {
            if (book < 1 || book > NEW_TESTAMENT_BOOKS) {
                throw new IllegalArgumentException("신약성경 권 번호는 1~27 사이여야 합니다.");
            }
            return NEW_TESTAMENT_CHAPTERS[book - 1];
        } else {
            throw new IllegalArgumentException("구분(구약=1, 신약=2) 값이 올바르지 않습니다.");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProgressSummary(Long userId) {
        if (userId == null) {
            return buildResponse(false, List.of());
        }
        List<BibleReadingProgress> records = progressRepository.findByUser_IdOrderByTestamentAscBookAscChapterAsc(userId);
        List<String> keys = records.stream().map(BibleReadingProgress::toKey).toList();
        return buildResponse(true, keys);
    }

    @Transactional
    public Map<String, Object> toggleChapter(Long userId, int testament, int book, int chapter, Boolean desiredState) {
        validateChapter(testament, book, chapter);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Optional<BibleReadingProgress> existing = progressRepository
                .findByUser_IdAndTestamentAndBookAndChapter(userId, testament, book, chapter);

        boolean nowRead;
        if (desiredState != null) {
            if (desiredState && existing.isEmpty()) {
                progressRepository.save(new BibleReadingProgress(user, testament, book, chapter));
            } else if (!desiredState && existing.isPresent()) {
                progressRepository.delete(existing.get());
            }
            nowRead = desiredState;
        } else {
            if (existing.isPresent()) {
                progressRepository.delete(existing.get());
                nowRead = false;
            } else {
                progressRepository.save(new BibleReadingProgress(user, testament, book, chapter));
                nowRead = true;
            }
        }
        progressRepository.flush();

        List<String> keys = progressRepository.findByUser_IdOrderByTestamentAscBookAscChapterAsc(userId)
                .stream()
                .map(BibleReadingProgress::toKey)
                .toList();

        Map<String, Object> response = new LinkedHashMap<>(buildResponse(true, keys));
        response.put("success", true);
        response.put("key", testament + "-" + book + "-" + chapter);
        response.put("read", nowRead);
        return response;
    }

    @Transactional
    public Map<String, Object> setBookProgress(Long userId, int testament, int book, boolean readAll) {
        int maxChapter = getMaxChapters(testament, book);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!readAll) {
            progressRepository.deleteByUser_IdAndTestamentAndBook(userId, testament, book);
        } else {
            List<BibleReadingProgress> existingList = progressRepository.findByUser_IdAndTestamentAndBook(userId, testament, book);
            Set<Integer> existingChapters = new HashSet<>();
            for (BibleReadingProgress item : existingList) {
                existingChapters.add(item.getChapter());
            }
            List<BibleReadingProgress> toSave = new ArrayList<>();
            for (int ch = 1; ch <= maxChapter; ch++) {
                if (!existingChapters.contains(ch)) {
                    toSave.add(new BibleReadingProgress(user, testament, book, ch));
                }
            }
            if (!toSave.isEmpty()) {
                progressRepository.saveAll(toSave);
            }
        }
        progressRepository.flush();

        List<String> keys = progressRepository.findByUser_IdOrderByTestamentAscBookAscChapterAsc(userId)
                .stream()
                .map(BibleReadingProgress::toKey)
                .toList();

        Map<String, Object> response = new LinkedHashMap<>(buildResponse(true, keys));
        response.put("success", true);
        response.put("testament", testament);
        response.put("book", book);
        response.put("read", readAll);
        return response;
    }

    public Map<String, Object> calculateStats(Collection<String> readKeys) {
        Set<String> uniqueKeys = new HashSet<>(readKeys);
        int oldRead = 0;
        int newRead = 0;
        int[] oldBookCounts = new int[OLD_TESTAMENT_BOOKS];
        int[] newBookCounts = new int[NEW_TESTAMENT_BOOKS];

        for (String key : uniqueKeys) {
            String[] parts = key.split("-");
            if (parts.length != 3) continue;
            try {
                int t = Integer.parseInt(parts[0]);
                int b = Integer.parseInt(parts[1]);
                int c = Integer.parseInt(parts[2]);
                if (t == 1 && b >= 1 && b <= OLD_TESTAMENT_BOOKS && c >= 1 && c <= OLD_TESTAMENT_CHAPTERS[b - 1]) {
                    oldRead++;
                    oldBookCounts[b - 1]++;
                } else if (t == 2 && b >= 1 && b <= NEW_TESTAMENT_BOOKS && c >= 1 && c <= NEW_TESTAMENT_CHAPTERS[b - 1]) {
                    newRead++;
                    newBookCounts[b - 1]++;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        int oldCompletedBooks = 0;
        for (int i = 0; i < OLD_TESTAMENT_BOOKS; i++) {
            if (oldBookCounts[i] >= OLD_TESTAMENT_CHAPTERS[i]) {
                oldCompletedBooks++;
            }
        }

        int newCompletedBooks = 0;
        for (int i = 0; i < NEW_TESTAMENT_BOOKS; i++) {
            if (newBookCounts[i] >= NEW_TESTAMENT_CHAPTERS[i]) {
                newCompletedBooks++;
            }
        }

        int totalRead = oldRead + newRead;
        int completedBooks = oldCompletedBooks + newCompletedBooks;

        double totalPercent = roundOneDecimal((totalRead * 100.0) / TOTAL_CHAPTERS);
        double oldPercent = roundOneDecimal((oldRead * 100.0) / OLD_TESTAMENT_TOTAL_CHAPTERS);
        double newPercent = roundOneDecimal((newRead * 100.0) / NEW_TESTAMENT_TOTAL_CHAPTERS);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalChapters", TOTAL_CHAPTERS);
        stats.put("totalRead", totalRead);
        stats.put("totalPercent", totalPercent);
        stats.put("totalBooks", TOTAL_BOOKS);
        stats.put("completedBooks", completedBooks);

        stats.put("oldTestamentTotal", OLD_TESTAMENT_TOTAL_CHAPTERS);
        stats.put("oldTestamentRead", oldRead);
        stats.put("oldTestamentPercent", oldPercent);
        stats.put("oldTestamentBooks", OLD_TESTAMENT_BOOKS);
        stats.put("oldTestamentCompletedBooks", oldCompletedBooks);

        stats.put("newTestamentTotal", NEW_TESTAMENT_TOTAL_CHAPTERS);
        stats.put("newTestamentRead", newRead);
        stats.put("newTestamentPercent", newPercent);
        stats.put("newTestamentBooks", NEW_TESTAMENT_BOOKS);
        stats.put("newTestamentCompletedBooks", newCompletedBooks);

        return stats;
    }

    private Map<String, Object> buildResponse(boolean authenticated, List<String> readChapters) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("authenticated", authenticated);
        map.put("readChapters", readChapters);
        map.put("stats", calculateStats(readChapters));
        return map;
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
