package com.guycode.tendenciaspos.identity.adapter.in.web;

import com.guycode.tendenciaspos.contracts.identity.AuthTokens;
import com.guycode.tendenciaspos.contracts.identity.SessionUser;
import com.guycode.tendenciaspos.contracts.identity.UserResponse;
import com.guycode.tendenciaspos.contracts.identity.UserRole;
import com.guycode.tendenciaspos.identity.application.Actor;
import com.guycode.tendenciaspos.identity.application.IssuedSession;
import com.guycode.tendenciaspos.identity.domain.Role;
import com.guycode.tendenciaspos.identity.domain.User;
import com.guycode.tendenciaspos.shared.security.TokenClaims;
import java.time.Clock;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;

/** Traducción entre el contrato público ({@code contracts}) y el dominio de identidad. */
final class IdentityMapper {
    private IdentityMapper() {}

    static Actor actor(Jwt jwt) {
        Number id = jwt.getClaim(TokenClaims.USER_ID);
        return new Actor(id.longValue(), jwt.getSubject());
    }

    static AuthTokens tokens(IssuedSession s) {
        return new AuthTokens(
                s.accessToken(),
                s.accessTokenExpiresAt(),
                s.refreshToken(),
                s.refreshTokenExpiresAt(),
                session(s.user()));
    }

    static SessionUser session(User u) {
        return new SessionUser(u.id(), u.username(), u.fullName(), toContract(u.roles()), u.mustChangePassword());
    }

    static UserResponse user(User u, Clock clock) {
        var lockedUntil = u.isLocked(clock.instant()) ? u.lockedUntil() : null;
        return new UserResponse(
                u.id(),
                u.username(),
                u.fullName(),
                toContract(u.roles()),
                u.active(),
                lockedUntil,
                u.mustChangePassword(),
                u.createdAt(),
                u.updatedAt());
    }

    static Set<Role> toDomain(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        var result = EnumSet.noneOf(Role.class);
        roles.forEach(r -> result.add(Role.valueOf(r.name())));
        return result;
    }

    private static Set<UserRole> toContract(Set<Role> roles) {
        return roles.stream().map(r -> UserRole.valueOf(r.name())).collect(Collectors.toUnmodifiableSet());
    }
}
