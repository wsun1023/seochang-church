package com.seochang.church.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bible_reading_progress", schema = "seochang_church_db",
       uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "testament", "book", "chapter"})})
public class BibleReadingProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int testament; // 1: 구약, 2: 신약

    @Column(nullable = false)
    private int book; // 구약 1~46, 신약 1~27

    @Column(nullable = false)
    private int chapter; // 1 ~ 각 권의 최대 장수

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt = LocalDateTime.now();

    public BibleReadingProgress() {
    }

    public BibleReadingProgress(User user, int testament, int book, int chapter) {
        this.user = user;
        this.testament = testament;
        this.book = book;
        this.chapter = chapter;
        this.readAt = LocalDateTime.now();
    }

    public String toKey() {
        return testament + "-" + book + "-" + chapter;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getTestament() {
        return testament;
    }

    public void setTestament(int testament) {
        this.testament = testament;
    }

    public int getBook() {
        return book;
    }

    public void setBook(int book) {
        this.book = book;
    }

    public int getChapter() {
        return chapter;
    }

    public void setChapter(int chapter) {
        this.chapter = chapter;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
