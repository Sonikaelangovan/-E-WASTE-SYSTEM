package com.loopcycle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.loopcycle.auth.AppUser;
import com.loopcycle.auth.UserRepository;
import com.loopcycle.auth.UserRole;
@SpringBootApplication
public class EwasteApplication {
 public static void main(String[] args) { SpringApplication.run(EwasteApplication.class, args); }

 @Bean ApplicationRunner bootstrapAdmin(UserRepository users, PasswordEncoder passwords,
     @Value("${app.auth.admin-email:}") String adminEmail,
     @Value("${app.auth.admin-password:}") String adminPassword) {
  return args -> {
   if (adminEmail.isBlank() && adminPassword.isBlank()) return;
   if (adminEmail.isBlank() || adminPassword.length() < 12 || adminPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
    throw new IllegalStateException("Set both ADMIN_EMAIL and a 12-72 character ADMIN_PASSWORD to provision the initial admin");
   String normalizedEmail = adminEmail.trim().toLowerCase(java.util.Locale.ROOT);
   if (!users.existsByEmail(normalizedEmail))
    users.save(new AppUser(normalizedEmail, passwords.encode(adminPassword), UserRole.ADMIN));
  };
 }
}
