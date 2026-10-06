package com.guycode.tendenciaspos.desktop.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;

/**
 * Configuración del escritorio. La URL de la API sale de la propiedad {@code -Dtpos.api.url}, de la
 * variable {@code TPOS_API_URL} o, por defecto, del servidor local.
 */
public record ClientConfig(URI apiUrl, Duration connectTimeout, Duration requestTimeout, String clientVersion) {
    public static final String DEFAULT_API_URL = "http://localhost:8080";

    public ClientConfig {
        Objects.requireNonNull(apiUrl, "apiUrl");
        Objects.requireNonNull(connectTimeout, "connectTimeout");
        Objects.requireNonNull(requestTimeout, "requestTimeout");
        Objects.requireNonNull(clientVersion, "clientVersion");
    }

    public static ClientConfig load() {
        var url = firstNonBlank(System.getProperty("tpos.api.url"), System.getenv("TPOS_API_URL"));
        return new ClientConfig(
                URI.create(url == null ? DEFAULT_API_URL : url),
                Duration.ofSeconds(5),
                Duration.ofSeconds(15),
                readClientVersion());
    }

    private static String readClientVersion() {
        try (InputStream in = ClientConfig.class.getResourceAsStream("/tpos-client.properties")) {
            if (in == null) {
                return "0.0.0";
            }
            var props = new Properties();
            props.load(in);
            return props.getProperty("version", "0.0.0");
        } catch (IOException e) {
            return "0.0.0";
        }
    }

    private static String firstNonBlank(String... values) {
        for (var value : values) {
            if (value != null && !value.isBlank()) {
                return value.strip();
            }
        }
        return null;
    }
}
