package com.guycode.tendenciaspos.identity.adapter.in.web;

import com.guycode.tendenciaspos.contracts.identity.AuthTokens;
import com.guycode.tendenciaspos.contracts.identity.ChangePasswordRequest;
import com.guycode.tendenciaspos.contracts.identity.LoginRequest;
import com.guycode.tendenciaspos.contracts.identity.RefreshTokenRequest;
import com.guycode.tendenciaspos.contracts.identity.SessionUser;
import com.guycode.tendenciaspos.identity.application.AuthService;
import com.guycode.tendenciaspos.identity.application.LoginResult;
import com.guycode.tendenciaspos.identity.application.RefreshResult;
import com.guycode.tendenciaspos.shared.web.BusinessException;
import com.guycode.tendenciaspos.shared.web.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Sesión: ingreso, renovación, salida, usuario actual y cambio de clave propia. */
@RestController
@RequestMapping("/api/auth")
class AuthController {
    private final AuthService auth;

    AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    AuthTokens login(@RequestBody LoginRequest body, HttpServletRequest request) {
        return switch (auth.login(body.username(), body.password(), request.getRemoteAddr())) {
            case LoginResult.Success s -> IdentityMapper.tokens(s.session());
            case LoginResult.InvalidCredentials ignored -> throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            case LoginResult.Locked l ->
                throw new BusinessException(
                        ErrorCode.ACCOUNT_LOCKED,
                        ErrorCode.ACCOUNT_LOCKED.defaultMessage(),
                        Map.of("lockedUntil", l.lockedUntil()));
        };
    }

    @PostMapping("/refresh")
    AuthTokens refresh(@RequestBody RefreshTokenRequest body, HttpServletRequest request) {
        return switch (auth.refresh(body.refreshToken(), request.getRemoteAddr())) {
            case RefreshResult.Success s -> IdentityMapper.tokens(s.session());
            case RefreshResult.Rejected ignored -> throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        };
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@RequestBody RefreshTokenRequest body) {
        auth.logout(body.refreshToken());
    }

    @GetMapping("/me")
    SessionUser me(@AuthenticationPrincipal Jwt jwt) {
        return IdentityMapper.session(auth.currentUser(IdentityMapper.actor(jwt)));
    }

    @PostMapping("/password")
    AuthTokens changePassword(@AuthenticationPrincipal Jwt jwt, @RequestBody ChangePasswordRequest body) {
        return IdentityMapper.tokens(
                auth.changePassword(IdentityMapper.actor(jwt), body.currentPassword(), body.newPassword()));
    }
}
