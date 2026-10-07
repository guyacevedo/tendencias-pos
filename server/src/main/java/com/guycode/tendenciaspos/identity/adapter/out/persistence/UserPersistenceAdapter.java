package com.guycode.tendenciaspos.identity.adapter.out.persistence;

import com.guycode.tendenciaspos.identity.application.UserRepository;
import com.guycode.tendenciaspos.identity.domain.Role;
import com.guycode.tendenciaspos.identity.domain.User;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
class UserPersistenceAdapter implements UserRepository {
    private final UserJpaRepository users;
    private final RoleJpaRepository roles;

    UserPersistenceAdapter(UserJpaRepository users, RoleJpaRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @Override
    public Optional<User> findById(long id) {
        return users.findById(id).map(UserPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<User> findByUsernameForUpdate(String username) {
        return users.findByUsernameForUpdate(username).map(UserPersistenceAdapter::toDomain);
    }

    @Override
    public List<User> findAll() {
        return users.findAllByOrderByUsernameAsc().stream()
                .map(UserPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public boolean existsByUsername(String username) {
        return users.existsByUsername(username);
    }

    @Override
    public long count() {
        return users.count();
    }

    @Override
    public long countActiveAdmins() {
        return users.countActiveWithRole(Role.ADMIN.name());
    }

    @Override
    public User save(User user) {
        var entity = user.id() == null
                ? new UserJpaEntity(user.username(), user.createdAt())
                : users.findById(user.id()).orElseThrow();
        entity.fullName = user.fullName();
        entity.passwordHash = user.passwordHash();
        entity.active = user.active();
        entity.failedAttempts = user.failedAttempts();
        entity.lockedUntil = user.lockedUntil();
        entity.mustChangePassword = user.mustChangePassword();
        entity.updatedAt = user.updatedAt();
        var codes = user.roles().stream().map(Enum::name).collect(Collectors.toSet());
        var current = entity.roles.stream().map(RoleJpaEntity::code).collect(Collectors.toSet());
        if (!codes.equals(current)) {
            entity.roles = new HashSet<>(roles.findByCodeIn(codes));
        }
        return toDomain(users.save(entity));
    }

    private static User toDomain(UserJpaEntity e) {
        var userRoles = EnumSet.noneOf(Role.class);
        e.roles.forEach(r -> userRoles.add(Role.valueOf(r.code())));
        return User.restore(
                e.id,
                e.username,
                e.fullName,
                e.passwordHash,
                e.active,
                e.failedAttempts,
                e.lockedUntil,
                e.mustChangePassword,
                userRoles,
                e.createdAt,
                e.updatedAt);
    }
}
