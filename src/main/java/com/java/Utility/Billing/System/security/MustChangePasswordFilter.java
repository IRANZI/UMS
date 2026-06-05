package com.java.Utility.Billing.System.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final List<String> ALLOWED_PATHS = List.of(
            "/api/auth/change-password",
            "/api/auth/logout",
            "/api/auth/refresh"
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.getPrincipal() instanceof UserPrincipal principal
                && principal.isMustChangePassword()) {

            String path = request.getRequestURI();
            boolean allowed = ALLOWED_PATHS.stream().anyMatch(path::equals);

            if (!allowed) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                String message = "You must change your temporary password before accessing the system. " +
                        "Use POST /api/auth/change-password";
                String json = """
                        {"status":403,"error":"Forbidden","message":"%s","path":"%s"}
                        """.formatted(escapeJson(message), escapeJson(path));
                response.getWriter().write(json);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
