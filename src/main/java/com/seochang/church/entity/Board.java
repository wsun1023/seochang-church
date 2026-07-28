package com.seochang.church.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "boards", schema = "seochang_church_db")
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String writer;

    @Column(name = "writer_id", nullable = false)
    private Long writerId;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "del_yn", nullable = false)
    private String delYn = "N";

    @Column(nullable = false, columnDefinition = "varchar(30) default 'free'")
    private String category = "free";

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<BoardAttachment> attachments = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    private java.util.List<BoardComment> comments = new java.util.ArrayList<>();

    public Board() {
    }

    public Board(String title, String content, String writer, Long writerId) {
        this.title = title;
        this.content = content;
        this.writer = writer;
        this.writerId = writerId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getWriter() {
        return writer;
    }

    public void setWriter(String writer) {
        this.writer = writer;
    }

    public Long getWriterId() {
        return writerId;
    }

    public void setWriterId(Long writerId) {
        this.writerId = writerId;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public String getDelYn() {
        return delYn;
    }

    public void setDelYn(String delYn) {
        this.delYn = delYn;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public java.util.List<BoardAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(java.util.List<BoardAttachment> attachments) {
        this.attachments = attachments;
    }

    public java.util.List<BoardComment> getComments() {
        return comments;
    }

    public void setComments(java.util.List<BoardComment> comments) {
        this.comments = comments;
    }
}
