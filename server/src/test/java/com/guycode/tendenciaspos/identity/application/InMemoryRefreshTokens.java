package com.guycode.tendenciaspos.identity.application;

import com.guycode.tendenciaspos.identity.domain.RefreshToken;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

class InMemoryRefreshTokens implements RefreshTokenRepository {
    private final Map<Long, RefreshToken> rows = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public RefreshToken save(RefreshToken t) {
        long id = t.id() == null ? nextId++ : t.id();
        var stored = RefreshToken.restore(id, t.userId(), t.tokenHash(), t.issuedAt(), t.expiresAt(), t.revokedAt());
        rows.put(id, stored);
        return copy(stored);
    }

    @Override
    public Optional<RefreshToken> findByHashForUpdate(String tokenHash) {
        return rows.values().stream()
                .filter(t -> t.tokenHash().equals(tokenHash))
                .findFirst()
                .map(InMemoryRefreshTokens::copy);
    }

    @Override
    public int revokeAllForUser(long userId, Instant now) {
        int count = 0;
        for (var t : rows.values()) {
            if (t.userId() == userId && !t.isRevoked()) {
                t.revoke(now);
                count++;
            }
        }
        return count;
    }

    long activeFor(long userId) {
        return rows.values().stream()
                .filter(t -> t.userId() == userId && !t.isRevoked())
                .count();
    }

    private static RefreshToken copy(RefreshToken t) {
        return RefreshToken.restore(t.id(), t.userId(), t.tokenHash(), t.issuedAt(), t.expiresAt(), t.revokedAt());
    }
}
