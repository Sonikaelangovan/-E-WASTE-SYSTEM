package com.loopcycle.item;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record LifecycleRequest(
        @NotNull LifecycleAction action,
        @Size(max = 600) String notes,
        Instant scheduledAt,
        @Email @Size(max = 254) String collectorEmail,
        @Email @Size(max = 254) String recyclerEmail,
        Boolean verified) {}
