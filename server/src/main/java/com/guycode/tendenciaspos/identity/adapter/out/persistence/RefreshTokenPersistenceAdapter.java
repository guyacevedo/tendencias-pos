package com.guycode.tendenciaspos.identity.adapter.out.persistence;

import com.guycode.tendenciaspos.identity.application.RefreshTokenRepository;
import com.guycode.tendenciaspos.identity.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class RefreshTokenPersistenceAdapter implements RefreshTokenRepository {
    private final RefreshTokenJpaRepository tokens;

    RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        var entity = token.id() == null
                ? new RefreshTokenJpaEntity(token.userId(), token.tokenHash(), token.issuedAt(), token.expiresAt())
                : tokens.findById(token.id()).orElseThrow();
        entity.revokedAt = token.revokedAt();
        return toDomain(tokens.save(entity));
    }

    @Override
    public Optional<RefreshToken> findByHashForUpdate(String tokenHash) {
        return tokens.findByHashForUpdate(tokenHash).map(RefreshTokenPersistenceAdapter::toDomain);
    }

    @Override
    public int revokeAllForUser(long userId, Instant now) {
        return tokens.revokeAllForUser(userId, now);
    }

    private static RefreshToken toDomain(RefreshTokenJpaEntity e) {
        return RefreshToken.restore(e.id, e.userId, e.tokenHash, e.issuedAt, e.expiresAt, e.revokedAt);
    }
}
