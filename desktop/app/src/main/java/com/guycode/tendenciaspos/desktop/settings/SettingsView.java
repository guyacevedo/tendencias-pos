package com.guycode.tendenciaspos.desktop.settings;

import com.guycode.tendenciaspos.contracts.settings.StoreSettingsResponse;
import java.util.Map;

/** Vista pasiva de la configuración de la tienda. Todos los métodos se llaman en el EDT. */
public interface SettingsView {
    void showLoading(boolean loading);

    void showSettings(StoreSettingsResponse settings);

    /** Mensajes por campo que rechazó el servidor. */
    void showFieldErrors(Map<String, String> errors);

    void showError(String message);

    void showSaved();
}
