package com.guycode.tendenciaspos.desktop.api;

import com.guycode.tendenciaspos.contracts.settings.StoreSettingsResponse;
import com.guycode.tendenciaspos.contracts.settings.UpdateStoreSettingsRequest;

/** Configuración de la tienda vista por el presentador. */
public interface SettingsGateway {
    StoreSettingsResponse get();

    StoreSettingsResponse update(UpdateStoreSettingsRequest request);
}
