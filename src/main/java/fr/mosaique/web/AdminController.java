package fr.mosaique.web;

import fr.mosaique.accounts.AccountStatus;
import fr.mosaique.accounts.Admin;
import fr.mosaique.accounts.SessionAccounts;
import fr.mosaique.accounts.User;
import fr.mosaique.accounts.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final SessionAccounts sessions;

    public AdminController(UserRepository users, SessionAccounts sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> accounts(HttpServletRequest request) {
        ResponseEntity<?> denied = adminRequired(request);
        if (denied != null) return denied;
        List<AccountResponse> accounts = users.findAll().stream().map(AdminController::toResponse).toList();
        return ResponseEntity.ok(accounts);
    }

    @PatchMapping("/accounts/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest payload, HttpServletRequest request) {
        ResponseEntity<?> denied = adminRequired(request);
        if (denied != null) return denied;
        User target = users.findById(id).orElse(null);
        if (target == null) return ResponseEntity.notFound().build();
        if (target instanceof Admin) return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthController.ErrorResponse("admin_account_protected"));
        target.setStatus(payload.status());
        return ResponseEntity.ok(toResponse(users.save(target)));
    }

    private ResponseEntity<?> adminRequired(HttpServletRequest request) {
        User current = sessions.current(request);
        if (current == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthController.ErrorResponse("authentication_required"));
        if (!(current instanceof Admin)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AuthController.ErrorResponse("admin_required"));
        return null;
    }

    private static AccountResponse toResponse(User user) {
        return new AccountResponse(user.getId(), user.getDisplayName(), user.getEmail(), user instanceof Admin, user.getStatus().name(), user.getCreatedAt());
    }

    public record StatusRequest(@NotNull AccountStatus status) { }
    public record AccountResponse(Long id, String displayName, String email, boolean admin, String status, Instant createdAt) { }
}