package com.seochang.church.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gallery", schema = "seochang_church_db")
public class Gallery {

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

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "gallery", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private java.util.List<GalleryAttachment> attachments = new java.util.ArrayList<>();

    public Gallery() {
    }

    public Gallery(String title, String content, String writer, Long writerId) {
        this.title = title;
        this.content = content;
        this.writer = writer;
        this.writerId = writerId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getWriter() { return writer; }
    public void setWriter(String writer) { this.writer = writer; }

    public Long getWriterId() { return writerId; }
    public void setWriterId(Long writerId) { this.writerId = writerId; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public String getDelYn() { return delYn; }
    public void setDelYn(String delYn) { this.delYn = delYn; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public java.util.List<GalleryAttachment> getAttachments() { return attachments; }
    public void setAttachments(java.util.List<GalleryAttachment> attachments) { this.attachments = attachments; }

    @Transient
    public java.util.List<GalleryAttachment> getPhotos() {
        return attachments.stream().filter(GalleryAttachment::isImage)
                .sorted(java.util.Comparator.comparingInt(GalleryAttachment::getSortOrder)
                        .thenComparing(GalleryAttachment::getId, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
    }

    @Transient
    public GalleryAttachment getCoverPhoto() {
        var photos = getPhotos();
        return photos.stream().filter(GalleryAttachment::isCoverPhoto).findFirst()
                .orElse(photos.isEmpty() ? null : photos.get(0));
    }
}
