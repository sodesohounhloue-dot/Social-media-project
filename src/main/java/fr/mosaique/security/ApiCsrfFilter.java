package fr.mosaique.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiCsrfFilter extends OncePerRequestFilter {
    public static final String TOKEN_ATTRIBUTE = "mosaique.csrfToken";
    private final SecureRandom random = new SecureRandom();

    public String getOrCreateToken(jakarta.servlet.http.HttpSession session) {
        String token = (String) session.getAttribute(TOKEN_ATTRIBUTE);
        if (token == null) {
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(TOKEN_ATTRIBUTE, token);
        }
        return token;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/")
            || request.getMethod().equals("GET")
            || request.getMethod().equals("HEAD")
            || request.getMethod().equals("OPTIONS");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        var session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(TOKEN_ATTRIBUTE);
        String provided = request.getHeader("X-CSRF-Token");
        if (expected == null || provided == null || !MessageDigest.isEqual(expected.getBytes(), provided.getBytes())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"csrf_validation_failed\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}