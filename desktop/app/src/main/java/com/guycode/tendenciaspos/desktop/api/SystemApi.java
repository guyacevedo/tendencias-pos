package com.guycode.tendenciaspos.desktop.api;

import com.guycode.tendenciaspos.contracts.ApiVersion;

/** Endpoints generales del servidor. */
public final class SystemApi {
    public static final String VERSION_PATH = "/api/version";

    private final ApiClient client;

    public SystemApi(ApiClient client) {
        this.client = client;
    }

    public ApiVersion version() {
        return client.get(VERSION_PATH, ApiVersion.class);
    }
}
