package com.guycode.tendenciaspos.identity.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserJpaEntity u where u.username = :username")
    Optional<UserJpaEntity> findByUsernameForUpdate(@Param("username") String username);

    boolean existsByUsername(String username);

    List<UserJpaEntity> findAllByOrderByUsernameAsc();

    @Query("select count(distinct u) from UserJpaEntity u join u.roles r where u.active = true and r.code = :role")
    long countActiveWithRole(@Param("role") String role);
}
