package com.guycode.tendenciaspos.desktop.login;

/** Vista pasiva del ingreso. Todos los métodos se llaman en el EDT. */
public interface LoginView {
    void showBusy(boolean busy);

    void showError(String message);

    void clearError();

    void clearPassword();
}
