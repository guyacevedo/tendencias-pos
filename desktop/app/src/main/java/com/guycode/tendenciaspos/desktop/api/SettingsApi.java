package com.guycode.tendenciaspos.desktop.api;

import com.guycode.tendenciaspos.contracts.settings.StoreSettingsResponse;
import com.guycode.tendenciaspos.contracts.settings.UpdateStoreSettingsRequest;

/** Endpoints de configuración de la tienda ({@code /api/settings}). */
public final class SettingsApi implements SettingsGateway {
    public static final String SETTINGS_PATH = "/api/settings";

    private final ApiClient client;

    public SettingsApi(ApiClient client) {
        this.client = client;
    }

    @Override
    public StoreSettingsResponse get() {
        return client.get(SETTINGS_PATH, StoreSettingsResponse.class);
    }

    @Override
    public StoreSettingsResponse update(UpdateStoreSettingsRequest request) {
        return client.put(SETTINGS_PATH, request, StoreSettingsResponse.class);
    }
}
