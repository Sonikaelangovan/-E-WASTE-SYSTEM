package com.loopcycle.notification;

import com.loopcycle.item.EwasteItem;
import com.loopcycle.item.ItemStatus;
import com.loopcycle.item.LifecycleAction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    public NotificationService(NotificationRepository notifications) { this.notifications = notifications; }

    @Transactional
    public void submissionCreated(EwasteItem item) {
        if (item.getOwnerEmail() == null) return;
        notifications.save(new AppNotification(item.getOwnerEmail(), item.getId(), "SUBMISSION",
                "Submission received", "Your item " + item.getTraceId() + " has been registered."));
    }

    @Transactional
    public void lifecycleChanged(EwasteItem item, LifecycleAction action) {
        Set<String> recipients = new LinkedHashSet<>();
        if (item.getOwnerEmail() != null) recipients.add(item.getOwnerEmail());
        if (action == LifecycleAction.SCHEDULE_PICKUP && item.getAssignedCollectorEmail() != null)
            recipients.add(item.getAssignedCollectorEmail());
        if (action == LifecycleAction.ASSIGN_RECYCLER && item.getAssignedRecyclerEmail() != null)
            recipients.add(item.getAssignedRecyclerEmail());
        ItemStatus status = item.getStatus();
        String title = "Item status updated";
        if (action == LifecycleAction.SCHEDULE_PICKUP) title = "Pickup scheduled";
        else if (action == LifecycleAction.ASSIGN_RECYCLER) title = "Item assigned for verification";
        else if (status == ItemStatus.REJECTED) title = "Item verification rejected";
        else if (status == ItemStatus.RECYCLED) title = "Recycling completed";
        else if (status == ItemStatus.DISPOSED) title = "Safe disposal recorded";
        String message = item.getTraceId() + " is now " + status.name().toLowerCase().replace('_', ' ') + ".";
        for (String recipient : recipients)
            notifications.save(new AppNotification(recipient, item.getId(), "LIFECYCLE", title, message));
    }
}
