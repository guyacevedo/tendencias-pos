package com.guycode.tendenciaspos.desktop.users;

import com.guycode.tendenciaspos.contracts.identity.UserResponse;
import java.util.List;
import java.util.Map;

/** Vista pasiva de la administración de usuarios. Todos los métodos se llaman en el EDT. */
public interface UsersView {
    void showLoading(boolean loading);

    void showUsers(List<UserResponse> users);

    /** Formulario con el usuario seleccionado, o vacío para crear uno nuevo si es {@code null}. */
    void showForm(UserResponse user);

    void showFieldErrors(Map<String, String> errors);

    void showError(String message);

    void showMessage(String message);
}
