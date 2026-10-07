package com.guycode.tendenciaspos.desktop.login;

import static org.assertj.core.api.Assertions.assertThat;

import com.guycode.tendenciaspos.contracts.identity.AuthTokens;
import com.guycode.tendenciaspos.contracts.identity.SessionUser;
import com.guycode.tendenciaspos.contracts.identity.UserRole;
import com.guycode.tendenciaspos.desktop.api.ApiException;
import com.guycode.tendenciaspos.desktop.core.UiExecutor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LoginPresenterTest {
    private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");

    private final RecordingView view = new RecordingView();
    private final UiExecutor direct = new UiExecutor(Runnable::run, Runnable::run);
    private final List<AuthTokens> authenticated = new ArrayList<>();

    @Test
    void exigeUsuarioYClaveSinLlamarALaApi() {
        var calls = new ArrayList<String>();
        presenter((user, pass) -> {
                    calls.add(user);
                    return tokens();
                })
                .submit("  ", "clave-larga-1".toCharArray());

        assertThat(calls).isEmpty();
        assertThat(view.calls).containsExactly("error:" + LoginPresenter.EMPTY_FIELDS);
    }

    @Test
    void entregaLaSesionYLimpiaLaClave() {
        presenter((user, pass) -> {
                    assertThat(user).isEqualTo("admin");
                    assertThat(pass).isEqualTo("clave-larga-1");
                    return tokens();
                })
                .submit("  admin  ", "clave-larga-1".toCharArray());

        assertThat(view.calls).containsExactly("clearError", "busy:true", "busy:false", "clearPassword");
        assertThat(authenticated).hasSize(1);
    }

    @Test
    void borraLaClaveDelArregloRecibido() {
        var password = "clave-larga-1".toCharArray();
        presenter((user, pass) -> tokens()).submit("admin", password);

        assertThat(password).containsOnly('\0');
    }

    @Test
    void muestraElMensajeDelServidorAlFallarLasCredenciales() {
        presenter((user, pass) -> {
                    throw new ApiException("INVALID_CREDENTIALS", "Usuario o clave incorrectos.", 401);
                })
                .submit("admin", "mala-clave-1".toCharArray());

        assertThat(view.calls).endsWith("error:Usuario o clave incorrectos.");
    }

    @Test
    void informaHastaQueHoraEstaBloqueadaLaCuenta() {
        presenter((user, pass) -> {
                    throw new ApiException(
                            "ACCOUNT_LOCKED",
                            "La cuenta está bloqueada.",
                            423,
                            Map.of("lockedUntil", "2026-10-06T19:35:00Z"),
                            null);
                })
                .submit("admin", "mala-clave-1".toCharArray());

        assertThat(view.calls).endsWith("error:" + LoginPresenter.LOCKED + " Vuelva a intentar después de las 14:35.");
    }

    @Test
    void sinConexionAvisaQueNoHayServidor() {
        presenter((user, pass) -> {
                    throw new ApiException(ApiException.NETWORK_ERROR, "detalle técnico", 0);
                })
                .submit("admin", "clave-larga-1".toCharArray());

        assertThat(view.calls).endsWith("error:No se pudo contactar al servidor.");
    }

    private LoginPresenter presenter(LoginPresenter.Login login) {
        return new LoginPresenter(view, login, direct, authenticated::add);
    }

    private static AuthTokens tokens() {
        var user = new SessionUser(1, "admin", "Administradora", Set.of(UserRole.ADMIN), false);
        return new AuthTokens("access", NOW, "refresh", NOW, user);
    }

    private static final class RecordingView implements LoginView {
        final List<String> calls = new ArrayList<>();

        @Override
        public void showBusy(boolean busy) {
            calls.add("busy:" + busy);
        }

        @Override
        public void showError(String message) {
            calls.add("error:" + message);
        }

        @Override
        public void clearError() {
            calls.add("clearError");
        }

        @Override
        public void clearPassword() {
            calls.add("clearPassword");
        }
    }
}
