package com.loopcycle.item;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record ItemRequest(@NotBlank @Size(max=120) String name,@NotBlank @Size(max=80) String category,@NotNull @DecimalMin("0.01") @DecimalMax("100000") BigDecimal weightKg,@NotBlank @Size(max=300) String address,@Size(max=1000) String description) {}
