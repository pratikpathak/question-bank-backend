package com.example.quiz.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import com.example.quiz.shared.AuthUser;
import com.example.quiz.shared.SessionUsers;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String USER_SESSION_ATTRIBUTE = SessionUsers.USER_SESSION_ATTRIBUTE;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final JdbcTemplate jdbcTemplate;
    private final PasswordHasher passwordHasher;
    private final String operatorSignupCode;

    public AuthController(
            JdbcTemplate jdbcTemplate,
            PasswordHasher passwordHasher,
            @Value("${app.operator-signup-code:}") String operatorSignupCode) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordHasher = passwordHasher;
        this.operatorSignupCode = operatorSignupCode;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@RequestBody Map<String, String> payload) {
        SignupDetails details = validateSignup(payload);
        if (details == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "Error",
                    "message", "Enter a valid email and a password with at least 8 characters."
            ));
        }

        try {
            jdbcTemplate.update(
                    "INSERT INTO LOGIN_DETAILS (email, password_hash, role) VALUES (?, ?, 'LEARNER')",
                    details.email(),
                    passwordHasher.hash(details.password())
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "Success",
                    "message", "Learner account created. You can now log in."
            ));
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "Error",
                    "message", "An account with this email already exists."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "Error",
                    "message", "Could not create the account."
            ));
        }
    }

    @PostMapping("/operator-signup")
    public ResponseEntity<Map<String, String>> operatorSignup(@RequestBody Map<String, String> payload) {
        SignupDetails details = validateSignup(payload);
        String providedCode = payload.get("signupCode");
        if (details == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "Error",
                    "message", "Enter a valid email and a password with at least 8 characters."
            ));
        }
        if (operatorSignupCode.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "status", "Error",
                    "message", "Operator signup is not enabled. Contact the application administrator."
            ));
        }
        if (providedCode == null || !MessageDigest.isEqual(
                operatorSignupCode.getBytes(StandardCharsets.UTF_8),
                providedCode.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "Error",
                    "message", "Invalid operator signup code."
            ));
        }

        try {
            jdbcTemplate.update(
                    "INSERT INTO LOGIN_DETAILS (email, password_hash, role) VALUES (?, ?, 'OPERATOR')",
                    details.email(),
                    passwordHasher.hash(details.password())
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "Success",
                    "message", "Operator account created. You can now sign in."
            ));
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "Error",
                    "message", "An account with this email already exists."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "Error",
                    "message", "Could not create the operator account."
            ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) {
        String email = normalizeEmail(payload.get("email"));
        String password = payload.get("password");
        if (email == null || password == null || password.length() > 128) {
            return invalidCredentials();
        }

        try {
            var users = jdbcTemplate.query(
                    "SELECT id, email, password_hash, role FROM LOGIN_DETAILS WHERE email = ?",
                    (rs, rowNum) -> new LoginRow(
                            rs.getLong("id"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            rs.getString("role")
                    ),
                    email
            );
            if (users.isEmpty()) {
                return invalidCredentials();
            }
            LoginRow user = users.get(0);
            if (!passwordHasher.matches(password, user.passwordHash())
                    || !isSupportedRole(user.role())) {
                return invalidCredentials();
            }

            HttpSession previousSession = request.getSession(false);
            if (previousSession != null) {
                previousSession.invalidate();
            }
            request.getSession(true);
            request.changeSessionId();
            request.getSession(true).setAttribute(
                    USER_SESSION_ATTRIBUTE,
                    new AuthUser(user.id(), user.email(), user.role())
            );
            return ResponseEntity.ok(Map.of(
                    "status", "Success",
                    "email", user.email(),
                    "role", user.role()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "Error",
                    "message", "Could not sign in."
            ));
        }
    }

    @GetMapping("/me")
    public Map<String, Object> currentUser(HttpSession session) {
        AuthUser user = SessionUsers.currentUser(session);
        if (user != null) {
            return Map.of(
                    "authenticated", true,
                    "email", user.email(),
                    "role", user.role()
            );
        }
        return Map.of("authenticated", false);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("status", "Success", "message", "Signed out."));
    }

    private ResponseEntity<Map<String, String>> invalidCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "status", "Error",
                "message", "Invalid email or password."
        ));
    }

    private boolean isSupportedRole(String role) {
        return "ADMIN".equals(role) || "DATA_ENTRY".equals(role)
                || "OPERATOR".equals(role) || "LEARNER".equals(role);
    }

    private SignupDetails validateSignup(Map<String, String> payload) {
        String email = normalizeEmail(payload.get("email"));
        String password = payload.get("password");
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()
                || email.length() > 255 || password == null
                || password.length() < 8 || password.length() > 128) {
            return null;
        }
        return new SignupDetails(email, password);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private record LoginRow(long id, String email, String passwordHash, String role) {
    }

    private record SignupDetails(String email, String password) {
    }
}
