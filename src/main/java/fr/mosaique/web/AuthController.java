package fr.mosaique.web;

import fr.mosaique.accounts.AccountStatus;
import fr.mosaique.accounts.Admin;
import fr.mosaique.accounts.SessionAccounts;
import fr.mosaique.accounts.User;
import fr.mosaique.accounts.UserRepository;
import fr.mosaique.security.ApiCsrfFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final SessionAccounts sessions;
    private final PasswordEncoder passwords;
    private final ApiCsrfFilter csrf;

    public AuthController(UserRepository users, SessionAccounts sessions, PasswordEncoder passwords, ApiCsrfFilter csrf) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
        this.csrf = csrf;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(HttpServletRequest request) {
        return new CsrfResponse(csrf.getOrCreateToken(request.getSession(true)));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        User account = sessions.current(request);
        return account == null
            ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("authentication_required"))
            : ResponseEntity.ok(toResponse(account));
    }

    @GetMapping("/admin-check")
    public AdminCheckResponse adminCheck(HttpServletRequest request) {
        User account = sessions.current(request);
        return new AdminCheckResponse(account != null, account instanceof Admin);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest payload, HttpServletRequest request) {
        String email = payload.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("email_already_registered"));
        }
        User user;
        try {
            user = users.saveAndFlush(new User(payload.displayName().trim(), email, passwords.encode(payload.password())));
        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("email_already_registered"));
        }
        authenticate(request, user);
        return ResponseEntity.created(URI.create("/api/auth/me")).body(toResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest payload, HttpServletRequest request) {
        User user = users.findByEmailIgnoreCase(payload.email().trim().toLowerCase()).orElse(null);
        if (user == null || !passwords.matches(payload.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("invalid_credentials"));
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("account_suspended"));
        }
        authenticate(request, user);
        return ResponseEntity.ok(toResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.noContent().build();
    }

    private static void authenticate(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(SessionAccounts.ACCOUNT_ID, user.getId());
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getDisplayName(), user.getEmail(), user instanceof Admin, user.getStatus().name(), user.getCreatedAt());
    }

    public record RegisterRequest(
        @NotBlank @Size(max = 80) String displayName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 72) String password
    ) { }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }
    public record UserResponse(Long id, String displayName, String email, boolean admin, String status, Instant createdAt) { }
    public record CsrfResponse(String token) { }
    public record AdminCheckResponse(boolean authenticated, boolean admin) { }
    public record ErrorResponse(String error) { }
}