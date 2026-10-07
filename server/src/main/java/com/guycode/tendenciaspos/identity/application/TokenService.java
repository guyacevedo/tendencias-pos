package com.guycode.tendenciaspos.identity.application;

import com.guycode.tendenciaspos.identity.domain.RefreshToken;
import com.guycode.tendenciaspos.identity.domain.User;
import com.guycode.tendenciaspos.shared.security.TokenClaims;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Emite access tokens JWT y refresh tokens opacos (32 bytes aleatorios; se guarda solo su SHA-256). */
@Component
public class TokenService {
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokens;
    private final IdentityProperties props;
    private final SecureRandom random = new SecureRandom();

    TokenService(JwtEncoder jwtEncoder, RefreshTokenRepository refreshTokens, IdentityProperties props) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokens = refreshTokens;
        this.props = props;
    }

    /** Sesión nueva que dura {@code refreshTokenTtl}. */
    IssuedSession startSession(User user, Instant now) {
        return issue(user, now, now.plus(props.refreshTokenTtl()));
    }

    /** Emite un par de tokens; el refresh token vence en {@code sessionEnd}. */
    IssuedSession issue(User user, Instant now, Instant sessionEnd) {
        var accessExpiresAt = now.plus(props.accessTokenTtl());
        var claims = JwtClaimsSet.builder()
                .issuer(TokenClaims.ISSUER)
                .subject(user.username())
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(accessExpiresAt)
                .claim(TokenClaims.USER_ID, user.id())
                .claim(
                        TokenClaims.ROLES,
                        user.roles().stream().map(Enum::name).sorted().toList())
                .claim(TokenClaims.PASSWORD_CHANGE, user.mustChangePassword())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var accessToken =
                jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        var refreshToken = newOpaqueToken();
        refreshTokens.save(RefreshToken.issue(user.id(), hash(refreshToken), now, sessionEnd));
        return new IssuedSession(accessToken, accessExpiresAt, refreshToken, sessionEnd, user);
    }

    private String newOpaqueToken() {
        var bytes = new byte[REFRESH_TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal: basta porque el token ya es aleatorio de 256 bits (no necesita sal). */
    static String hash(String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
