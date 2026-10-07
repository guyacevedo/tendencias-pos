package com.guycode.tendenciaspos.identity.application;

import com.guycode.tendenciaspos.identity.domain.Role;
import com.guycode.tendenciaspos.identity.domain.User;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repositorio en memoria; guarda copias para que los cambios sin {@code save} no se filtren. */
class InMemoryUsers implements UserRepository {
    private final Map<Long, User> rows = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public Optional<User> findById(long id) {
        return Optional.ofNullable(rows.get(id)).map(InMemoryUsers::copy);
    }

    @Override
    public Optional<User> findByUsernameForUpdate(String username) {
        return rows.values().stream()
                .filter(u -> u.username().equals(username))
                .findFirst()
                .map(InMemoryUsers::copy);
    }

    @Override
    public List<User> findAll() {
        return rows.values().stream()
                .sorted(Comparator.comparing(User::username))
                .map(InMemoryUsers::copy)
                .toList();
    }

    @Override
    public boolean existsByUsername(String username) {
        return rows.values().stream().anyMatch(u -> u.username().equals(username));
    }

    @Override
    public long count() {
        return rows.size();
    }

    @Override
    public long countActiveAdmins() {
        return rows.values().stream()
                .filter(u -> u.active() && u.roles().contains(Role.ADMIN))
                .count();
    }

    @Override
    public User save(User user) {
        long id = user.id() == null ? nextId++ : user.id();
        var stored = User.restore(
                id,
                user.username(),
                user.fullName(),
                user.passwordHash(),
                user.active(),
                user.failedAttempts(),
                user.lockedUntil(),
                user.mustChangePassword(),
                user.roles(),
                user.createdAt(),
                user.updatedAt());
        rows.put(id, stored);
        return copy(stored);
    }

    private static User copy(User u) {
        return User.restore(
                u.id(),
                u.username(),
                u.fullName(),
                u.passwordHash(),
                u.active(),
                u.failedAttempts(),
                u.lockedUntil(),
                u.mustChangePassword(),
                u.roles(),
                u.createdAt(),
                u.updatedAt());
    }
}
