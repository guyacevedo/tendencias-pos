package com.guycode.tendenciaspos.desktop.home;

/** Vista pasiva de la pantalla de inicio. Todos los métodos se llaman en el EDT. */
public interface HomeView {
    void showLoading(boolean loading);

    void showVersions(String clientVersion, String serverVersion);

    /** El servidor exige una versión del escritorio más nueva. */
    void showUpdateRequired(String minClientVersion);

    void showError(String message);
}
