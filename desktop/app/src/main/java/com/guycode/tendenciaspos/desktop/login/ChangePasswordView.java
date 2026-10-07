package com.guycode.tendenciaspos.desktop.login;

/** Vista pasiva del cambio de clave. Todos los métodos se llaman en el EDT. */
public interface ChangePasswordView {
    void showBusy(boolean busy);

    void showError(String message);

    void clearError();

    void clearFields();
}
