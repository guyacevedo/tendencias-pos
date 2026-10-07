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
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChangePasswordPresenterTest {
    private final RecordingView view = new RecordingView();
    private final UiExecutor direct = new UiExecutor(Runnable::run, Runnable::run);
    private final List<AuthTokens> changed = new ArrayList<>();

    @Test
    void validaAntesDeLlamarALaApi() {
        var calls = new ArrayList<String>();
        ChangePasswordPresenter.ChangePassword api = (current, next) -> {
            calls.add(next);
            return tokens();
        };

        submit(api, "actual-1234", "", "");
        submit(api, "actual-1234", "corta", "corta");
        submit(api, "actual-1234", "clave-nueva-1", "clave-nueva-2");
        submit(api, "clave-nueva-1", "clave-nueva-1", "clave-nueva-1");

        assertThat(calls).isEmpty();
        assertThat(view.calls)
                .containsExactly(
                        "error:" + ChangePasswordPresenter.EMPTY_FIELDS,
                        "error:" + ChangePasswordPresenter.TOO_SHORT,
                        "error:" + ChangePasswordPresenter.MISMATCH,
                        "error:" + ChangePasswordPresenter.SAME_PASSWORD);
    }

    @Test
    void entregaLaSesionNuevaYBorraLosCampos() {
        submit((current, next) -> tokens(), "actual-12345", "clave-nueva-1", "clave-nueva-1");

        assertThat(view.calls).containsExactly("clearError", "busy:true", "busy:false", "clearFields");
        assertThat(changed).hasSize(1);
    }

    @Test
    void muestraElMensajeDelServidor() {
        submit(
                (current, next) -> {
                    throw new ApiException("CURRENT_PASSWORD_INCORRECT", "La clave actual no es correcta.", 400);
                },
                "actual-12345",
                "clave-nueva-1",
                "clave-nueva-1");

        assertThat(view.calls).endsWith("error:La clave actual no es correcta.");
    }

    private void submit(ChangePasswordPresenter.ChangePassword api, String current, String next, String confirm) {
        new ChangePasswordPresenter(view, api, direct, changed::add)
                .submit(current.toCharArray(), next.toCharArray(), confirm.toCharArray());
    }

    private static AuthTokens tokens() {
        var now = Instant.parse("2026-10-06T12:00:00Z");
        var user = new SessionUser(1, "admin", "Administradora", Set.of(UserRole.ADMIN), false);
        return new AuthTokens("access", now, "refresh", now, user);
    }

    private static final class RecordingView implements ChangePasswordView {
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
        public void clearFields() {
            calls.add("clearFields");
        }
    }
}
