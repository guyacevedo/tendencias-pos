package com.guycode.tendenciaspos.identity.domain;

import java.time.Instant;
import java.util.Objects;

/** Token de renovación guardado por su hash. Un solo uso: al renovar se revoca y se emite otro. */
public final class RefreshToken {
    private final Long id;
    private final long userId;
    private final String tokenHash;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private Instant revokedAt;

    private RefreshToken(
            Long id, long userId, String tokenHash, Instant issuedAt, Instant expiresAt, Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.issuedAt = Objects.requireNonNull(issuedAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.revokedAt = revokedAt;
    }

    public static RefreshToken issue(long userId, String tokenHash, Instant now, Instant expiresAt) {
        return new RefreshToken(null, userId, tokenHash, now, expiresAt, null);
    }

    public static RefreshToken restore(
            long id, long userId, String tokenHash, Instant issuedAt, Instant expiresAt, Instant revokedAt) {
        return new RefreshToken(id, userId, tokenHash, issuedAt, expiresAt, revokedAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public Long id() {
        return id;
    }

    public long userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }
}
