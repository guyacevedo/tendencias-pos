package com.guycode.tendenciaspos.desktop.auth;

import com.guycode.tendenciaspos.contracts.identity.AuthTokens;
import com.guycode.tendenciaspos.contracts.identity.SessionUser;
import com.guycode.tendenciaspos.contracts.identity.UserRole;
import com.guycode.tendenciaspos.desktop.api.ApiClient;
import com.guycode.tendenciaspos.desktop.api.ApiException;
import com.guycode.tendenciaspos.desktop.api.AuthApi;
import com.guycode.tendenciaspos.desktop.api.SessionGateway;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Sesión en memoria: nada se guarda en disco. Entrega la cabecera {@code Authorization} de cada llamada
 * y, cuando al access token le queda poco, lo renueva antes de salir. Las renovaciones están
 * serializadas porque el refresh token es de un solo uso: usarlo dos veces cierra todas las sesiones.
 */
public final class SessionManager implements ApiClient.Authorization {
    /** Mensaje con el que termina una sesión que el servidor ya no acepta. */
    public static final String SESSION_ENDED = "La sesión terminó. Inicie sesión de nuevo.";

    /** Código local del error que se lanza cuando la sesión ya no sirve. */
    public static final String SESSION_ENDED_CODE = "SESSION_ENDED";

    /** Se renueva el access token cuando le queda menos de este margen. */
    static final Duration RENEW_MARGIN = Duration.ofSeconds(60);

    private final SessionGateway gateway;
    private final Clock clock;
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private final Object lock = new Object();
    private AuthTokens tokens;

    public SessionManager(SessionGateway gateway, Clock clock) {
        this.gateway = gateway;
        this.clock = clock;
    }

    /** Avisa, fuera del EDT, que la sesión terminó sin que el usuario la cerrara. */
    public void onSessionEnded(Consumer<String> listener) {
        listeners.add(listener);
    }

    public void start(AuthTokens issued) {
        synchronized (lock) {
            tokens = issued;
        }
    }

    public boolean isActive() {
        synchronized (lock) {
            return tokens != null;
        }
    }

    /** Usuario de la sesión, o {@code null} si no hay sesión. */
    public SessionUser user() {
        synchronized (lock) {
            return tokens == null ? null : tokens.user();
        }
    }

    public Set<UserRole> roles() {
        var user = user();
        return user == null ? Set.of() : user.roles();
    }

    public boolean hasRole(UserRole role) {
        return roles().contains(role);
    }

    /** Cierra la sesión en el servidor; los errores de red no impiden salir. */
    public void logout() {
        AuthTokens current;
        synchronized (lock) {
            current = tokens;
            tokens = null;
        }
        if (current != null) {
            try {
                gateway.logout(current.refreshToken());
            } catch (ApiException e) {
                // La sesión local ya quedó cerrada; el refresh token vence solo.
            }
        }
    }

    @Override
    public String headerFor(String path) {
        if (AuthApi.PUBLIC_PATHS.contains(path)) {
            return null;
        }
        AuthTokens usable;
        synchronized (lock) {
            if (tokens == null) {
                return null;
            }
            if (clock.instant().isBefore(tokens.accessTokenExpiresAt().minus(RENEW_MARGIN))) {
                return bearer(tokens);
            }
            try {
                tokens = gateway.refresh(tokens.refreshToken());
                usable = tokens;
            } catch (ApiException e) {
                if (e.isConnectivity()) {
                    throw e;
                }
                tokens = null;
                usable = null;
            }
        }
        if (usable == null) {
            notifyEnded();
            throw new ApiException(SESSION_ENDED_CODE, SESSION_ENDED, 401);
        }
        return bearer(usable);
    }

    @Override
    public void rejected(String path) {
        if (AuthApi.PUBLIC_PATHS.contains(path)) {
            return;
        }
        boolean wasActive;
        synchronized (lock) {
            wasActive = tokens != null;
            tokens = null;
        }
        if (wasActive) {
            notifyEnded();
        }
    }

    private void notifyEnded() {
        listeners.forEach(listener -> listener.accept(SESSION_ENDED));
    }

    private static String bearer(AuthTokens current) {
        return "Bearer " + current.accessToken();
    }
}
