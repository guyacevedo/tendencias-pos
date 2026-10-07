package com.guycode.tendenciaspos.identity.domain;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Usuario del sistema. Nunca se borra: se desactiva. */
public final class User {
    private final Long id;
    private final String username;
    private String fullName;
    private String passwordHash;
    private boolean active;
    private int failedAttempts;
    private Instant lockedUntil;
    private boolean mustChangePassword;
    private Set<Role> roles;
    private final Instant createdAt;
    private Instant updatedAt;

    private User(
            Long id,
            String username,
            String fullName,
            String passwordHash,
            boolean active,
            int failedAttempts,
            Instant lockedUntil,
            boolean mustChangePassword,
            Set<Role> roles,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username");
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.active = active;
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
        this.mustChangePassword = mustChangePassword;
        this.roles = copyRoles(roles);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** Usuario nuevo, activo, que debe cambiar la clave en su primer ingreso. */
    public static User create(String username, String fullName, String passwordHash, Set<Role> roles, Instant now) {
        return new User(null, username, fullName, passwordHash, true, 0, null, true, roles, now, now);
    }

    /** Reconstruye un usuario guardado. */
    public static User restore(
            long id,
            String username,
            String fullName,
            String passwordHash,
            boolean active,
            int failedAttempts,
            Instant lockedUntil,
            boolean mustChangePassword,
            Set<Role> roles,
            Instant createdAt,
            Instant updatedAt) {
        return new User(
                id,
                username,
                fullName,
                passwordHash,
                active,
                failedAttempts,
                lockedUntil,
                mustChangePassword,
                roles,
                createdAt,
                updatedAt);
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /**
     * Cuenta un intento fallido; al llegar al máximo bloquea la cuenta y reinicia el contador.
     *
     * @return {@code true} si este intento bloqueó la cuenta
     */
    public boolean registerFailedLogin(Instant now, LockoutPolicy policy) {
        if (lockedUntil != null && !isLocked(now)) {
            lockedUntil = null;
        }
        failedAttempts++;
        updatedAt = now;
        if (failedAttempts >= policy.maxFailedAttempts()) {
            failedAttempts = 0;
            lockedUntil = now.plus(policy.lockDuration());
            return true;
        }
        return false;
    }

    public void registerSuccessfulLogin(Instant now) {
        if (failedAttempts != 0 || lockedUntil != null) {
            failedAttempts = 0;
            lockedUntil = null;
            updatedAt = now;
        }
    }

    /** El propio usuario cambió su clave. */
    public void changePassword(String newHash, Instant now) {
        passwordHash = Objects.requireNonNull(newHash);
        mustChangePassword = false;
        updatedAt = now;
    }

    /** Un administrador asignó una clave temporal: se desbloquea y debe cambiarla al entrar. */
    public void resetPassword(String newHash, Instant now) {
        passwordHash = Objects.requireNonNull(newHash);
        mustChangePassword = true;
        failedAttempts = 0;
        lockedUntil = null;
        updatedAt = now;
    }

    /** Hash recalculado con parámetros más fuertes; no cambia la clave. */
    public void upgradePasswordHash(String newHash) {
        passwordHash = Objects.requireNonNull(newHash);
    }

    public void update(String newFullName, Set<Role> newRoles, Instant now) {
        fullName = Objects.requireNonNull(newFullName);
        roles = copyRoles(newRoles);
        updatedAt = now;
    }

    public void deactivate(Instant now) {
        active = false;
        updatedAt = now;
    }

    public void activate(Instant now) {
        active = true;
        failedAttempts = 0;
        lockedUntil = null;
        updatedAt = now;
    }

    public boolean isActiveAdmin() {
        return active && roles.contains(Role.ADMIN);
    }

    private static Set<Role> copyRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Un usuario necesita al menos un rol");
        }
        return Set.copyOf(EnumSet.copyOf(roles));
    }

    public Long id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String fullName() {
        return fullName;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public boolean active() {
        return active;
    }

    public int failedAttempts() {
        return failedAttempts;
    }

    public Instant lockedUntil() {
        return lockedUntil;
    }

    public boolean mustChangePassword() {
        return mustChangePassword;
    }

    public Set<Role> roles() {
        return roles;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
