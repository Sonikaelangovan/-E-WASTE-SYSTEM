package com.loopcycle.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final TokenService tokens;

    public AuthController(UserRepository users, PasswordEncoder passwords, TokenService tokens) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody Credentials credentials) {
        if (passwordBytes(credentials.password()) > 72) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        String email = normalize(credentials.email());
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        AppUser user = users.save(new AppUser(email, passwords.encode(credentials.password()), UserRole.CITIZEN));
        return response(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody Credentials credentials) {
        if (passwordBytes(credentials.password()) > 72) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        AppUser user = users.findByEmail(normalize(credentials.email()))
                .filter(candidate -> passwords.matches(credentials.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return response(user);
    }

    @GetMapping("/me")
    public UserResponse me(org.springframework.security.core.Authentication authentication) {
        AppUser user = users.findByEmail(authentication.getName()).orElseThrow();
        return new UserResponse(user.getEmail(), user.getRole());
    }

    private AuthResponse response(AppUser user) {
        return new AuthResponse(tokens.issue(user), new UserResponse(user.getEmail(), user.getRole()));
    }
    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private int passwordBytes(String password) { return password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length; }

    public record Credentials(@NotBlank @Email @Size(max = 254) String email,
                              @NotBlank @Size(min = 12, max = 72) String password) {}
    public record UserResponse(String email, UserRole role) {}
    public record AuthResponse(String token, UserResponse user) {}
}
