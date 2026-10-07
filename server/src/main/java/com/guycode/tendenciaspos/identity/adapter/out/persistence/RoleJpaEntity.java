package com.guycode.tendenciaspos.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Catálogo fijo de roles, sembrado por la migración V2. */
@Entity
@Immutable
@Table(name = "role")
class RoleJpaEntity {
    @Id
    private Short id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    protected RoleJpaEntity() {}

    String code() {
        return code;
    }
}
