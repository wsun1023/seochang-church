package com.seochang.church.repository;

import com.seochang.church.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientId(Long recipientId, Pageable pageable);
    Page<Notification> findByRecipientIdAndReadAtIsNull(Long recipientId, Pageable pageable);
    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);
    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.id = :id and n.recipientId = :recipient and n.readAt is null")
    int markRead(@Param("id") Long id, @Param("recipient") Long recipient, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.recipientId = :recipient and n.readAt is null")
    int markAllRead(@Param("recipient") Long recipient, @Param("now") LocalDateTime now);
}
