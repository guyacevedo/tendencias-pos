package com.guycode.tendenciaspos.shared.security;

import com.guycode.tendenciaspos.shared.web.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/** Mientras el token diga que la clave debe cambiarse, solo se permiten las rutas de la propia cuenta. */
class PasswordChangeRequiredFilter extends OncePerRequestFilter {
    static final Set<String> ALLOWED = Set.of(
            "/api/auth/password",
            "/api/auth/me",
            "/api/auth/logout",
            "/api/auth/refresh",
            "/api/auth/login",
            "/api/version");

    private final ProblemDetailsSecurityHandler handler;

    PasswordChangeRequiredFilter(ProblemDetailsSecurityHandler handler) {
        this.handler = handler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token
                && Boolean.TRUE.equals(token.getToken().getClaimAsBoolean(TokenClaims.PASSWORD_CHANGE))
                && !ALLOWED.contains(request.getRequestURI())) {
            handler.write(request, response, ErrorCode.PASSWORD_CHANGE_REQUIRED);
            return;
        }
        chain.doFilter(request, response);
    }
}
