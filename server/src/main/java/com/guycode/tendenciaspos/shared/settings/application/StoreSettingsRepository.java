package com.guycode.tendenciaspos.shared.settings.application;

import com.guycode.tendenciaspos.shared.settings.domain.StoreSettings;

/** Puerto de persistencia de la única fila de configuración. */
public interface StoreSettingsRepository {
    StoreSettings load();

    StoreSettings save(StoreSettings settings);
}
