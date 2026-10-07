package com.guycode.tendenciaspos.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "refresh_token")
class RefreshTokenJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    long userId;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    Instant issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    Instant expiresAt;

    @Column(name = "revoked_at")
    Instant revokedAt;

    protected RefreshTokenJpaEntity() {}

    RefreshTokenJpaEntity(long userId, String tokenHash, Instant issuedAt, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }
}
