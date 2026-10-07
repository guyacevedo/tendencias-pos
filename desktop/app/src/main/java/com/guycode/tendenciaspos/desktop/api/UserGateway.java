package com.guycode.tendenciaspos.desktop.api;

import com.guycode.tendenciaspos.contracts.identity.UserResponse;
import com.guycode.tendenciaspos.contracts.identity.UserRole;
import java.util.List;
import java.util.Set;

/** Administración de usuarios vista por el presentador (solo ADMIN). */
public interface UserGateway {
    List<UserResponse> list();

    UserResponse create(String username, String fullName, String password, Set<UserRole> roles);

    UserResponse update(long id, String fullName, Set<UserRole> roles);

    void resetPassword(long id, String newPassword);

    UserResponse deactivate(long id);

    UserResponse activate(long id);
}
