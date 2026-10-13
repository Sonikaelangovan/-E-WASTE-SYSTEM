package com.loopcycle.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findTop50ByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);
    long countByRecipientEmailAndReadAtIsNull(String recipientEmail);
    Optional<AppNotification> findByIdAndRecipientEmail(Long id, String recipientEmail);
    List<AppNotification> findAllByRecipientEmailAndReadAtIsNull(String recipientEmail);
}
