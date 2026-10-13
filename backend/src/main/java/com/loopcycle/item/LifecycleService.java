package com.loopcycle.item;

import com.loopcycle.auth.AppUser;
import com.loopcycle.auth.UserRepository;
import com.loopcycle.auth.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.loopcycle.notification.NotificationService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
public class LifecycleService {
    private final ItemRepository items;
    private final UserRepository users;
    private final NotificationService notifications;

    public LifecycleService(ItemRepository items, UserRepository users, NotificationService notifications) {
        this.items = items;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional
    public ItemResponse apply(Long itemId, LifecycleRequest request, Authentication actor) {
        EwasteItem item = items.findByIdForUpdate(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        String email = actor.getName();
        UserRole role = role(actor);
        String notes = clean(request.notes());

        switch (request.action()) {
            case SCHEDULE_PICKUP -> {
                requireRole(role, UserRole.COLLECTOR, UserRole.ADMIN);
                requireStatus(item, ItemStatus.SUBMITTED);
                Instant scheduledAt = request.scheduledAt();
                if (scheduledAt == null || !scheduledAt.isAfter(Instant.now()) || scheduledAt.isAfter(Instant.now().plus(90, ChronoUnit.DAYS)))
                    throw badRequest("Pickup time must be in the future and within 90 days");
                String collectorEmail = role == UserRole.ADMIN ? normalizeRequired(request.collectorEmail(), "collectorEmail") : email;
                requireUserRole(collectorEmail, UserRole.COLLECTOR, "Assigned account must have the COLLECTOR role");
                item.schedulePickup(collectorEmail, scheduledAt);
                item.record(ItemStatus.PICKUP_SCHEDULED, email,
                        append("Pickup scheduled for " + scheduledAt + "; collector " + collectorEmail, notes));
            }
            case CONFIRM_COLLECTION -> {
                requireRole(role, UserRole.COLLECTOR, UserRole.ADMIN);
                requireStatus(item, ItemStatus.PICKUP_SCHEDULED);
                requireAssignedOrAdmin(item.getAssignedCollectorEmail(), email, role, "Only the assigned collector can confirm pickup");
                item.record(ItemStatus.COLLECTED, email, defaultNote(notes, "Item collected"));
            }
            case SUBMIT_FOR_VERIFICATION -> {
                requireRole(role, UserRole.COLLECTOR, UserRole.ADMIN);
                requireStatus(item, ItemStatus.COLLECTED);
                requireAssignedOrAdmin(item.getAssignedCollectorEmail(), email, role, "Only the assigned collector can submit this item for verification");
                item.record(ItemStatus.AWAITING_VERIFICATION, email, defaultNote(notes, "Item received and submitted for verification"));
            }
            case ASSIGN_RECYCLER -> {
                requireRole(role, UserRole.ADMIN);
                requireStatus(item, ItemStatus.AWAITING_VERIFICATION);
                String recyclerEmail = normalizeRequired(request.recyclerEmail(), "recyclerEmail");
                requireUserRole(recyclerEmail, UserRole.RECYCLER, "Assigned account must have the RECYCLER role");
                item.assignRecycler(recyclerEmail);
                item.record(ItemStatus.RECYCLER_ASSIGNED, email, append("Assigned to recycler " + recyclerEmail, notes));
            }
            case VERIFY -> {
                requireRole(role, UserRole.RECYCLER, UserRole.ADMIN);
                requireStatus(item, ItemStatus.RECYCLER_ASSIGNED);
                requireAssignedOrAdmin(item.getAssignedRecyclerEmail(), email, role, "Only the assigned recycler can verify this item");
                if (request.verified() == null) throw badRequest("verified must be true or false");
                if (!request.verified() && notes.isBlank()) throw badRequest("A reason is required when verification is rejected");
                item.record(request.verified() ? ItemStatus.VERIFIED : ItemStatus.REJECTED, email,
                        defaultNote(notes, "Recycler verification passed"));
            }
            case START_RECYCLING -> {
                requireRole(role, UserRole.RECYCLER, UserRole.ADMIN);
                requireStatus(item, ItemStatus.VERIFIED);
                requireAssignedOrAdmin(item.getAssignedRecyclerEmail(), email, role, "Only the assigned recycler can start processing");
                item.record(ItemStatus.RECYCLING, email, defaultNote(notes, "Recycling started"));
            }
            case COMPLETE_RECYCLING -> {
                requireRole(role, UserRole.RECYCLER, UserRole.ADMIN);
                requireStatus(item, ItemStatus.RECYCLING);
                requireAssignedOrAdmin(item.getAssignedRecyclerEmail(), email, role, "Only the assigned recycler can complete processing");
                item.record(ItemStatus.RECYCLED, email, defaultNote(notes, "Recycling completed"));
            }
            case SAFE_DISPOSAL -> {
                requireRole(role, UserRole.RECYCLER, UserRole.ADMIN);
                requireStatus(item, ItemStatus.VERIFIED);
                requireAssignedOrAdmin(item.getAssignedRecyclerEmail(), email, role, "Only the assigned recycler can record disposal");
                if (notes.isBlank()) throw badRequest("Describe the safe disposal method");
                item.record(ItemStatus.DISPOSED, email, notes);
            }
        }
        items.save(item);
        notifications.lifecycleChanged(item, request.action());
        return ItemResponse.from(item);
    }

    private UserRole role(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(granted -> granted.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> UserRole.valueOf(authority.substring(5)))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Account role required"));
    }

    private void requireStatus(EwasteItem item, ItemStatus expected) {
        if (item.getStatus() != expected)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Action requires status " + expected + "; current status is " + item.getStatus());
    }

    private void requireRole(UserRole actual, UserRole... allowed) {
        for (UserRole role : allowed) if (role == actual) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your role cannot perform this lifecycle action");
    }

    private void requireAssignedOrAdmin(String assignedEmail, String actorEmail, UserRole role, String message) {
        if (role == UserRole.ADMIN) return;
        if (assignedEmail == null || !assignedEmail.equalsIgnoreCase(actorEmail))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private void requireUserRole(String email, UserRole expected, String message) {
        AppUser user = users.findByEmail(email).orElseThrow(() -> badRequest("Assigned account does not exist"));
        if (user.getRole() != expected) throw badRequest(message);
    }

    private String normalizeRequired(String email, String field) {
        if (email == null || email.isBlank()) throw badRequest(field + " is required");
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String clean(String notes) { return notes == null ? "" : notes.trim(); }
    private String defaultNote(String notes, String fallback) { return notes.isBlank() ? fallback : notes; }
    private String append(String summary, String notes) { return notes.isBlank() ? summary : summary + " — " + notes; }
    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
