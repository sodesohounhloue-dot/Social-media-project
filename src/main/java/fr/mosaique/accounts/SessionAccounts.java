package fr.mosaique.accounts;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class SessionAccounts {
    public static final String ACCOUNT_ID = "mosaique.accountId";
    private final UserRepository users;

    public SessionAccounts(UserRepository users) {
        this.users = users;
    }

    public User current(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object id = session.getAttribute(ACCOUNT_ID);
        if (!(id instanceof Long accountId)) return null;
        User account = users.findById(accountId).orElse(null);
        if (account == null || account.getStatus() != AccountStatus.ACTIVE) {
            session.invalidate();
            return null;
        }
        return account;
    }
}