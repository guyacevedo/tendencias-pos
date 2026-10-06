package com.guycode.tendenciaspos.desktop.api;

import com.guycode.tendenciaspos.desktop.core.ClientConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import tools.jackson.core.JacksonException;
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

    private record ProblemBody(String code, String title, String detail) {}

    private final String baseUrl;
    private final Duration requestTimeout;
    private final String clientVersion;
    private final ConnectionObserver observer;
    private final HttpClient http;
    private final JsonMapper json;

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

    /** {@code GET path}; {@code type} puede ser {@code Void.class} si no hay cuerpo. */
    public <T> T get(String path, Class<T> type) {
        return send(request(path).GET().build(), type);
    }

    public <T> T post(String path, Object body, Class<T> type) {
        return send(withBody(request(path), "POST", body), type);
    }

    public <T> T put(String path, Object body, Class<T> type) {
        return send(withBody(request(path), "PUT", body), type);
    }

    public void delete(String path) {
        send(request(path).DELETE().build(), Void.class);
    }

    private HttpRequest.Builder request(String path) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("La ruta debe empezar con '/': " + path);
        }
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(requestTimeout)
                .header("Accept", "application/json, application/problem+json")
                .header(CLIENT_VERSION_HEADER, clientVersion);
    }

    private HttpRequest withBody(HttpRequest.Builder builder, String method, Object body) {
        return builder.header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body)))
                .build();
    }

    private <T> T send(HttpRequest request, Class<T> type) {
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
        var code = problem != null && problem.code() != null ? problem.code() : "HTTP_" + status;
        String message;
        if (problem != null && problem.detail() != null && !problem.detail().isBlank()) {
            message = problem.detail();
        } else if (problem != null
                && problem.title() != null
                && !problem.title().isBlank()) {
            message = problem.title();
        } else if (status >= 500) {
            message = "El servidor tuvo un problema (HTTP " + status + ").";
        } else {
            message = "La solicitud no se pudo completar (HTTP " + status + ").";
        }
        return new ApiException(code, message, status);
    }

    private ProblemBody parseProblem(HttpResponse<byte[]> response) {
        var contentType = response.headers().firstValue("Content-Type").orElse("");
        if (!contentType.contains("json") || response.body().length == 0) {
            return null;
        }
        try {
            return json.readValue(response.body(), ProblemBody.class);
        } catch (JacksonException e) {
            return null;
        }
    }

    @Override
    public void close() {
        http.close();
    }
}
