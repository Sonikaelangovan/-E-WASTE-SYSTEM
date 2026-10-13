package com.loopcycle.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class TokenService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final ObjectMapper mapper;
    private final byte[] secret;
    private final long lifetimeSeconds;

    public TokenService(ObjectMapper mapper, @Value("${app.auth.jwt-secret:}") String secret,
                        @Value("${app.auth.token-lifetime-seconds:28800}") long lifetimeSeconds) {
        this.mapper = mapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.lifetimeSeconds = lifetimeSeconds;
        if (this.secret.length < 32) throw new IllegalStateException("APP_JWT_SECRET must contain at least 32 characters");
    }

    public String issue(AppUser user) {
        try {
            String header = encode(mapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = encode(mapper.writeValueAsBytes(Map.of(
                    "sub", user.getEmail(), "role", user.getRole().name(),
                    "exp", Instant.now().getEpochSecond() + lifetimeSeconds)));
            String content = header + "." + payload;
            return content + "." + ENCODER.encodeToString(sign(content));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not issue access token", exception);
        }
    }

    public TokenPrincipal verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            Map<?, ?> header = mapper.readValue(DECODER.decode(parts[0]), Map.class);
            if (!"HS256".equals(header.get("alg"))) return null;
            byte[] actual = DECODER.decode(parts[2]);
            if (!java.security.MessageDigest.isEqual(sign(parts[0] + "." + parts[1]), actual)) return null;
            Map<?, ?> claims = mapper.readValue(DECODER.decode(parts[1]), Map.class);
            String email = (String) claims.get("sub");
            UserRole role = UserRole.valueOf((String) claims.get("role"));
            long expiry = ((Number) claims.get("exp")).longValue();
            if (email == null || expiry <= Instant.now().getEpochSecond()) return null;
            return new TokenPrincipal(email, role);
        } catch (Exception exception) {
            return null;
        }
    }

    private String encode(byte[] bytes) { return ENCODER.encodeToString(bytes); }
    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.US_ASCII));
    }

    public record TokenPrincipal(String email, UserRole role) {}
}
