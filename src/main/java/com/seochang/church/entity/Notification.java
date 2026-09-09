package com.seochang.church.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", schema = "seochang_church_db", indexes = {
        @Index(name = "idx_notifications_recipient_read_created", columnList = "recipient_id,read_at,created_at")
})
public class Notification {
    public enum Type {
        NOTICE("새 공지"), COMMENT("댓글"), REPLY("답글"), APPROVAL("회원 승인");
        private final String label;
        Type(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Type type;
    @Column(nullable = false, length = 500)
    private String message;
    @Column(name = "target_id")
    private Long targetId;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected Notification() {}
    public Notification(Long recipientId, Type type, String message, Long targetId) {
        this.recipientId = recipientId;
        this.type = type;
        this.message = message;
        this.targetId = targetId;
    }
    public Long getId() { return id; }
    public Long getRecipientId() { return recipientId; }
    public Type getType() { return type; }
    public String getMessage() { return message; }
    public Long getTargetId() { return targetId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public boolean isRead() { return readAt != null; }
}
