package com.guycode.tendenciaspos.shared.security;

import com.guycode.tendenciaspos.shared.web.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

/** Errores de autenticación/autorización del filtro de seguridad como Problem Details con {@code code}. */
class ProblemDetailsSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final JsonMapper json;

    ProblemDetailsSecurityHandler(JsonMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(request, response, isExpired(ex) ? ErrorCode.TOKEN_EXPIRED : ErrorCode.UNAUTHORIZED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(request, response, ErrorCode.FORBIDDEN);
    }

    void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code) throws IOException {
        var body = new LinkedHashMap<String, Object>();
        body.put("type", "about:blank");
        body.put("title", code.status().getReasonPhrase());
        body.put("status", code.status().value());
        body.put("detail", code.defaultMessage());
        body.put("instance", request.getRequestURI());
        body.put("code", code.name());
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.writeValueAsString(body));
    }

    /** El validador de fechas de Spring Security marca así el token vencido. */
    private static boolean isExpired(Throwable ex) {
        for (var t = ex; t != null; t = t.getCause()) {
            if (t instanceof JwtValidationException jve) {
                return jve.getErrors().stream()
                        .anyMatch(e ->
                                e.getDescription() != null && e.getDescription().contains("expired"));
            }
        }
        return false;
    }
}
