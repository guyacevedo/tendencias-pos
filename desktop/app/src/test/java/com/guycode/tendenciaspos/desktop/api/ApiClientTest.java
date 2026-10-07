package com.guycode.tendenciaspos.desktop.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import com.guycode.tendenciaspos.contracts.ApiVersion;
import com.guycode.tendenciaspos.desktop.core.ClientConfig;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiClientTest implements ApiClient.ConnectionObserver {
    private final List<String> connectionEvents = new CopyOnWriteArrayList<>();
    private final CountDownLatch release = new CountDownLatch(1);
    private HttpServer server;
    private ApiClient client;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        client = new ApiClient(config("http://127.0.0.1:" + server.getAddress().getPort() + "/"), this);
    }

    @AfterEach
    void stop() {
        release.countDown();
        client.close();
        server.stop(0);
    }

    @Override
    public void reachable() {
        connectionEvents.add("up");
    }

    @Override
    public void unreachable() {
        connectionEvents.add("down");
    }

    @Test
    void getDeserializesJsonAndSendsClientVersion() {
        var header = new AtomicReference<String>();
        server.createContext("/api/version", exchange -> {
            header.set(exchange.getRequestHeaders().getFirst(ApiClient.CLIENT_VERSION_HEADER));
            reply(exchange, 200, "application/json", """
                    {"serverVersion":"0.1.0","minClientVersion":"0.1.0","extra":true}""");
        });

        var version = client.get("/api/version", ApiVersion.class);

        assertThat(version).isEqualTo(new ApiVersion("0.1.0", "0.1.0"));
        assertThat(header.get()).isEqualTo("1.2.3");
        assertThat(connectionEvents).containsExactly("up");
    }

    @Test
    void problemDetailsBecomeApiExceptionWithCode() {
        server.createContext("/api/sales", exchange -> reply(exchange, 409, "application/problem+json", """
                {"type":"about:blank","title":"Conflict","status":409,
                 "detail":"No hay stock suficiente.","code":"STOCK_INSUFICIENTE"}"""));

        assertThatThrownBy(() -> client.post("/api/sales", new ApiVersion("a", "b"), Void.class))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("STOCK_INSUFICIENTE");
                    assertThat(e.getMessage()).isEqualTo("No hay stock suficiente.");
                    assertThat(e.status()).isEqualTo(409);
                    assertThat(e.isConnectivity()).isFalse();
                });
        assertThat(connectionEvents).containsExactly("up");
    }

    @Test
    void errorWithoutProblemDetailsFallsBackToHttpCode() {
        server.createContext("/boom", exchange -> reply(exchange, 500, "text/html", "<h1>Error</h1>"));

        assertThatThrownBy(() -> client.get("/boom", Void.class)).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.code()).isEqualTo("HTTP_500");
            assertThat(e.getMessage()).contains("HTTP 500");
        });
    }

    @Test
    void gatewayErrorMeansServerUnavailable() {
        server.createContext("/api/version", exchange -> reply(exchange, 502, "text/html", "Bad Gateway"));

        assertThatThrownBy(() -> client.get("/api/version", ApiVersion.class))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo(ApiException.SERVER_UNAVAILABLE);
                    assertThat(e.isConnectivity()).isTrue();
                });
        assertThat(connectionEvents).containsExactly("down");
    }

    @Test
    void invalidJsonIsInvalidResponse() {
        server.createContext("/api/version", exchange -> reply(exchange, 200, "application/json", "no es json"));

        assertThatThrownBy(() -> client.get("/api/version", ApiVersion.class))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.code()).isEqualTo(ApiException.INVALID_RESPONSE));
    }

    @Test
    void refusedConnectionIsNetworkError() {
        int port = server.getAddress().getPort();
        server.stop(0);
        try (var offline = new ApiClient(config("http://127.0.0.1:" + port), this)) {
            assertThatThrownBy(() -> offline.get("/api/version", ApiVersion.class))
                    .isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.code()).isEqualTo(ApiException.NETWORK_ERROR);
                        assertThat(e.status()).isZero();
                    });
        }
        assertThat(connectionEvents).containsExactly("down");
    }

    @Test
    void slowServerTimesOutAsNetworkError() {
        server.createContext("/slow", exchange -> {
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            reply(exchange, 200, "application/json", "{}");
        });

        assertThatThrownBy(() -> client.get("/slow", Void.class))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.code()).isEqualTo(ApiException.NETWORK_ERROR));
    }

    @Test
    void sendsAuthorizationHeaderAndReportsRejection() {
        var rejected = new CopyOnWriteArrayList<String>();
        var sent = new AtomicReference<String>();
        client.setAuthorization(new ApiClient.Authorization() {
            @Override
            public String headerFor(String path) {
                return AuthApi.LOGIN_PATH.equals(path) ? null : "Bearer access-1";
            }

            @Override
            public void rejected(String path) {
                rejected.add(path);
            }
        });
        server.createContext("/api/users", exchange -> {
            sent.set(exchange.getRequestHeaders().getFirst("Authorization"));
            reply(exchange, 401, "application/problem+json", """
                    {"status":401,"detail":"La sesión expiró.","code":"TOKEN_EXPIRED"}""");
        });
        server.createContext(AuthApi.LOGIN_PATH, exchange -> {
            sent.set(exchange.getRequestHeaders().getFirst("Authorization"));
            reply(exchange, 401, "application/problem+json", """
                    {"status":401,"detail":"Usuario o clave incorrectos.","code":"INVALID_CREDENTIALS"}""");
        });

        assertThatThrownBy(() -> client.get("/api/users", Void.class)).isInstanceOf(ApiException.class);
        assertThat(sent.get()).isEqualTo("Bearer access-1");
        assertThat(rejected).containsExactly("/api/users");

        assertThatThrownBy(() -> client.post(AuthApi.LOGIN_PATH, "{}", Void.class))
                .isInstanceOf(ApiException.class);
        assertThat(sent.get()).isNull();
        assertThat(rejected).containsExactly("/api/users", AuthApi.LOGIN_PATH);
    }

    @Test
    void problemDetailsExtrasTravelInTheException() {
        server.createContext("/api/settings", exchange -> reply(exchange, 400, "application/problem+json", """
                {"status":400,"detail":"Hay datos inválidos en la solicitud.","code":"VALIDATION_FAILED",
                 "errors":{"storeName":"El nombre de la tienda es obligatorio."},
                 "lockedUntil":"2026-10-06T19:35:00Z"}"""));

        assertThatThrownBy(() -> client.put("/api/settings", "{}", Void.class))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.fieldErrors())
                            .containsExactly(entry("storeName", "El nombre de la tienda es obligatorio."));
                    assertThat(e.detail("lockedUntil")).isEqualTo("2026-10-06T19:35:00Z");
                    assertThat(e.detail("nada")).isNull();
                });
    }

    private static ClientConfig config(String url) {
        return new ClientConfig(URI.create(url), Duration.ofSeconds(2), Duration.ofMillis(500), "1.2.3");
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, int status, String type, String body)
            throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
