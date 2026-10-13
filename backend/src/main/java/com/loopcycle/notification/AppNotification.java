package com.loopcycle.notification;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_notification")
public class AppNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "recipient_email", nullable = false, length = 254)
    private String recipientEmail;
    @Column(name = "item_id")
    private Long itemId;
    @Column(name = "notification_type", nullable = false, length = 32)
    private String type;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(nullable = false, length = 500)
    private String message;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "read_at")
    private Instant readAt;

    protected AppNotification() {}
    public AppNotification(String recipientEmail, Long itemId, String type, String title, String message) {
        this.recipientEmail = recipientEmail;
        this.itemId = itemId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.createdAt = Instant.now();
    }
    public Long getId() { return id; }
    public Long getItemId() { return itemId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public void markRead() { if (readAt == null) readAt = Instant.now(); }
}
