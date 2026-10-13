package com.loopcycle.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserRepository users;
    public AdminUserController(UserRepository users) { this.users = users; }

    @GetMapping
    public List<AdminUserResponse> all() {
        return users.findAll().stream().map(user -> new AdminUserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt())).toList();
    }

    @PutMapping("/{id}/role")
    public AdminUserResponse changeRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request, Authentication actor) {
        AppUser user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getEmail().equals(actor.getName()) && request.role() != UserRole.ADMIN)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot remove your own admin role");
        user.setRole(request.role());
        user = users.save(user);
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }

    public record RoleRequest(@NotNull UserRole role) {}
    public record AdminUserResponse(Long id, String email, UserRole role, java.time.Instant createdAt) {}
}
