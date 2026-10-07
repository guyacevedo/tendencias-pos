package com.guycode.tendenciaspos.identity.application;

import com.guycode.tendenciaspos.identity.domain.User;
import java.util.List;
import java.util.Optional;

/** Puerto de salida: almacenamiento de usuarios. */
public interface UserRepository {
    Optional<User> findById(long id);

    /** Busca y bloquea la fila hasta el fin de la transacción (intentos de ingreso concurrentes). */
    Optional<User> findByUsernameForUpdate(String username);

    /** Todos, ordenados por nombre de usuario. */
    List<User> findAll();

    boolean existsByUsername(String username);

    long count();

    long countActiveAdmins();

    User save(User user);
}
