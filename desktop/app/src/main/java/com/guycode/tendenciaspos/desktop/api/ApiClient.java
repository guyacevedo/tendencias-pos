package com.guycode.tendenciaspos.desktop.api;

import com.guycode.tendenciaspos.desktop.core.ClientConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cliente HTTP de la API (JDK {@link HttpClient} + Jackson 3). Las llamadas son bloqueantes: hacerlas
 * fuera del EDT con {@link com.guycode.tendenciaspos.desktop.core.UiExecutor}. Los errores salen como
 * {@link ApiException} y el estado de la conexión se informa a un {@link ConnectionObserver}.
 */
public final class ApiClient implements AutoCloseable {
    public static final String CLIENT_VERSION_HEADER = "X-Client-Version";

    /** Recibe el resultado de red de cada llamada (desde el hilo que la hizo). */
    public interface ConnectionObserver {
        void reachable();

        void unreachable();

        ConnectionObserver NONE = new ConnectionObserver() {
            @Override
            public void reachable() {}

            @Override
            public void unreachable() {}
        };
    }

    /** Entrega la cabecera {@code Authorization} de cada ruta y se entera si la API la rechaza. */
    public interface Authorization {
        /** Valor de la cabecera, o {@code null} si la ruta es pública o todavía no hay sesión. */
        String headerFor(String path);

        /** La API respondió 401 a esta ruta. */
        default void rejected(String path) {}

        Authorization NONE = path -> null;
    }

    /** Campos estándar de RFC 9457 más {@code code}; el resto viaja en {@link ApiException#details()}. */
    private static final Set<String> PROBLEM_KEYS = Set.of("type", "title", "status", "detail", "instance", "code");

    private static final TypeReference<Map<String, Object>> PROBLEM_TYPE = new TypeReference<>() {};

    private final String baseUrl;
    private final Duration requestTimeout;
    private final String clientVersion;
    private final ConnectionObserver observer;
    private final HttpClient http;
    private final JsonMapper json;
    private volatile Authorization authorization = Authorization.NONE;

    public ApiClient(ClientConfig config, ConnectionObserver observer) {
        this.baseUrl = config.apiUrl().toString().replaceAll("/+$", "");
        this.requestTimeout = config.requestTimeout();
        this.clientVersion = config.clientVersion();
        this.observer = Objects.requireNonNull(observer, "observer");
        this.http = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.json = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    /** Credenciales de las llamadas siguientes; se define al armar la sesión. */
    public void setAuthorization(Authorization authorization) {
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    /** {@code GET path}; {@code type} puede ser {@code Void.class} si no hay cuerpo. */
    public <T> T get(String path, Class<T> type) {
        return send(path, request(path).GET().build(), type);
    }

    public <T> T post(String path, Object body, Class<T> type) {
        return send(path, withBody(request(path), "POST", body), type);
    }

    public <T> T put(String path, Object body, Class<T> type) {
        return send(path, withBody(request(path), "PUT", body), type);
    }

    public void delete(String path) {
        send(path, request(path).DELETE().build(), Void.class);
    }

    private HttpRequest.Builder request(String path) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("La ruta debe empezar con '/': " + path);
        }
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(requestTimeout)
                .header("Accept", "application/json, application/problem+json")
                .header(CLIENT_VERSION_HEADER, clientVersion);
        var credential = authorization.headerFor(path);
        return credential == null ? builder : builder.header("Authorization", credential);
    }

    private HttpRequest withBody(HttpRequest.Builder builder, String method, Object body) {
        return builder.header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body)))
                .build();
    }

    private <T> T send(String path, HttpRequest request, Class<T> type) {
        HttpResponse<byte[]> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            observer.unreachable();
            throw new ApiException(ApiException.NETWORK_ERROR, "No hay conexión con el servidor.", 0, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ApiException.INTERRUPTED, "La operación fue cancelada.", 0, e);
        }

        int status = response.statusCode();
        if (status == 502 || status == 503 || status == 504) {
            observer.unreachable();
            throw new ApiException(
                    ApiException.SERVER_UNAVAILABLE, "El servidor no está disponible en este momento.", status);
        }
        observer.reachable();
        if (status >= 200 && status < 300) {
            return read(response, type);
        }
        if (status == 401) {
            authorization.rejected(path);
        }
        throw toException(response);
    }

    private <T> T read(HttpResponse<byte[]> response, Class<T> type) {
        if (type == Void.class || response.body().length == 0) {
            return null;
        }
        try {
            return json.readValue(response.body(), type);
        } catch (JacksonException e) {
            throw new ApiException(
                    ApiException.INVALID_RESPONSE,
                    "El servidor envió una respuesta inesperada.",
                    response.statusCode(),
                    e);
        }
    }

    private ApiException toException(HttpResponse<byte[]> response) {
        int status = response.statusCode();
        var problem = parseProblem(response);
        var code = text(problem.get("code")) != null ? text(problem.get("code")) : "HTTP_" + status;
        String message;
        if (text(problem.get("detail")) != null) {
            message = text(problem.get("detail"));
        } else if (text(problem.get("title")) != null) {
            message = text(problem.get("title"));
        } else if (status >= 500) {
            message = "El servidor tuvo un problema (HTTP " + status + ").";
        } else {
            message = "La solicitud no se pudo completar (HTTP " + status + ").";
        }
        var extra = new LinkedHashMap<String, Object>();
        problem.forEach((key, value) -> {
            if (!PROBLEM_KEYS.contains(key) && value != null) {
                extra.put(key, value);
            }
        });
        return new ApiException(code, message, status, extra, null);
    }

    /** Cuerpo del Problem Details como mapa; vacío si la respuesta no trae JSON legible. */
    private Map<String, Object> parseProblem(HttpResponse<byte[]> response) {
        var contentType = response.headers().firstValue("Content-Type").orElse("");
        if (!contentType.contains("json") || response.body().length == 0) {
            return Map.of();
        }
        try {
            return json.readValue(response.body(), PROBLEM_TYPE);
        } catch (JacksonException e) {
            return Map.of();
        }
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        var result = String.valueOf(value);
        return result.isBlank() ? null : result;
    }

    @Override
    public void close() {
        http.close();
    }
}
