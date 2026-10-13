package com.loopcycle.notification;

import java.time.Instant;

public record NotificationResponse(Long id, Long itemId, String type, String title, String message,
                                   Instant createdAt, Instant readAt) {
    static NotificationResponse from(AppNotification notification) {
        return new NotificationResponse(notification.getId(), notification.getItemId(), notification.getType(),
                notification.getTitle(), notification.getMessage(), notification.getCreatedAt(), notification.getReadAt());
    }
}
