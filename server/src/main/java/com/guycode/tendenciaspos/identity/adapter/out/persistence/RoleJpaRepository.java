package com.guycode.tendenciaspos.identity.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, Short> {
    List<RoleJpaEntity> findByCodeIn(Collection<String> codes);
}
