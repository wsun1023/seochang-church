package com.seochang.church.service;

import com.seochang.church.entity.*;
import com.seochang.church.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final NoticeRepository notices;
    private final BoardRepository boards;

    public NotificationService(NotificationRepository notifications, UserRepository users,
                               NoticeRepository notices, BoardRepository boards) {
        this.notifications = notifications;
        this.users = users;
        this.notices = notices;
        this.boards = boards;
    }

    public void noticePublished(Notice notice) {
        for (Long recipient : users.findNotificationRecipientIds()) {
            notifications.save(new Notification(recipient, Notification.Type.NOTICE,
                    "새 공지가 등록되었습니다: " + notice.getTitle(), notice.getId()));
        }
    }

    public void memberApproved(User user) {
        if (active(user)) notifications.save(new Notification(user.getId(), Notification.Type.APPROVAL,
                "회원가입이 승인되었습니다. 이제 게시판을 이용하실 수 있습니다.", null));
    }

    public void commentAdded(BoardComment comment, BoardComment parent) {
        Map<Long, Notification.Type> recipients = new LinkedHashMap<>();
        recipients.put(comment.getBoard().getWriterId(), Notification.Type.COMMENT);
        if (parent != null) recipients.put(parent.getWriterId(), Notification.Type.REPLY);
        recipients.remove(comment.getWriterId());
        for (var entry : recipients.entrySet()) {
            if (entry.getKey() == null) continue;
            users.findById(entry.getKey()).filter(this::active).ifPresent(user -> {
                if ("Y".equals(comment.getSecretYn()) && !user.getId().equals(comment.getBoard().getWriterId())
                        && !"ADMIN".equals(user.getRole())) return;
                // Do not include comment bodies or secret contents in notifications.
                String message = entry.getValue() == Notification.Type.REPLY
                        ? "내 댓글에 답글이 등록되었습니다." : "내 게시글에 댓글이 등록되었습니다.";
                notifications.save(new Notification(user.getId(), entry.getValue(), message, comment.getBoard().getId()));
            });
        }
    }

    private boolean active(User user) { return user.isApproved() && "N".equals(user.getDelYn()); }

    @Transactional(readOnly = true)
    public Page<Notification> list(Long recipient, int page, boolean unreadOnly) {
        Pageable pageable = PageRequest.of(Math.max(0, page), 20, Sort.by("createdAt").descending().and(Sort.by("id").descending()));
        return unreadOnly ? notifications.findByRecipientIdAndReadAtIsNull(recipient, pageable)
                : notifications.findByRecipientId(recipient, pageable);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long recipient) { return notifications.countByRecipientIdAndReadAtIsNull(recipient); }

    private Notification owned(Long recipient, Long id) {
        return notifications.findByIdAndRecipientId(id, recipient)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."));
    }

    public void markRead(Long recipient, Long id) {
        owned(recipient, id);
        notifications.markRead(id, recipient, LocalDateTime.now());
    }

    public void markAllRead(Long recipient) { notifications.markAllRead(recipient, LocalDateTime.now()); }

    public String open(Long recipient, Long id) {
        Notification notification = owned(recipient, id);
        notifications.markRead(id, recipient, LocalDateTime.now());
        return switch (notification.getType()) {
            case APPROVAL -> "/boards";
            case NOTICE -> notices.existsById(notification.getTargetId()) ? "/notices/" + notification.getTargetId() : null;
            case COMMENT, REPLY -> boards.findById(notification.getTargetId()).filter(b -> "N".equals(b.getDelYn()))
                    .map(b -> "/boards/" + b.getId()).orElse(null);
        };
    }
}
