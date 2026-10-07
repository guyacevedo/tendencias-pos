package com.guycode.tendenciaspos.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
class UserJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 50, updatable = false)
    String username;

    @Column(name = "full_name", nullable = false, length = 120)
    String fullName;

    @Column(name = "password_hash", nullable = false)
    String passwordHash;

    @Column(nullable = false)
    boolean active;

    @Column(name = "failed_attempts", nullable = false)
    int failedAttempts;

    @Column(name = "locked_until")
    Instant lockedUntil;

    @Column(name = "must_change_password", nullable = false)
    boolean mustChangePassword;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    Set<RoleJpaEntity> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Version
    long version;

    protected UserJpaEntity() {}

    UserJpaEntity(String username, Instant createdAt) {
        this.username = username;
        this.createdAt = createdAt;
    }
}
