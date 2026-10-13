package com.loopcycle.notification;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository notifications;
    public NotificationController(NotificationRepository notifications) { this.notifications = notifications; }

    @GetMapping
    public InboxResponse inbox(Authentication authentication, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        String email = authentication.getName();
        List<NotificationResponse> recent = notifications.findTop50ByRecipientEmailOrderByCreatedAtDesc(email)
                .stream().map(NotificationResponse::from).toList();
        return new InboxResponse(recent, notifications.countByRecipientEmailAndReadAtIsNull(email));
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id, Authentication authentication) {
        AppNotification notification = notifications.findByIdAndRecipientEmail(id, authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.markRead();
        notifications.save(notification);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(Authentication authentication) {
        var unread = notifications.findAllByRecipientEmailAndReadAtIsNull(authentication.getName());
        unread.forEach(AppNotification::markRead);
        notifications.saveAll(unread);
    }

    public record InboxResponse(List<NotificationResponse> notifications, long unreadCount) {}
}
