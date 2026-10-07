package com.guycode.tendenciaspos.desktop.login;

import com.guycode.tendenciaspos.contracts.identity.AuthTokens;
import com.guycode.tendenciaspos.desktop.api.ApiException;
import com.guycode.tendenciaspos.desktop.core.ApiErrors;
import com.guycode.tendenciaspos.desktop.core.Dates;
import com.guycode.tendenciaspos.desktop.core.ErrorCodes;
import com.guycode.tendenciaspos.desktop.core.UiExecutor;
import java.util.Arrays;
import java.util.function.Consumer;

/** Valida el formulario de ingreso, llama a la API y entrega la sesión recién abierta. */
public final class LoginPresenter {
    static final String EMPTY_FIELDS = "Escriba su usuario y su clave.";
    static final String LOCKED = "La cuenta está bloqueada por intentos fallidos.";

    /** Ingreso contra la API; se ejecuta fuera del EDT. */
    public interface Login {
        AuthTokens login(String username, String password);
    }

    private final LoginView view;
    private final Login login;
    private final UiExecutor executor;
    private final Consumer<AuthTokens> onAuthenticated;

    public LoginPresenter(LoginView view, Login login, UiExecutor executor, Consumer<AuthTokens> onAuthenticated) {
        this.view = view;
        this.login = login;
        this.executor = executor;
        this.onAuthenticated = onAuthenticated;
    }

    /** La clave se borra del arreglo en cuanto se arma la petición. */
    public void submit(String username, char[] password) {
        var user = username == null ? "" : username.strip();
        var secret = password == null ? "" : new String(password);
        if (password != null) {
            Arrays.fill(password, '\0');
        }
        if (user.isEmpty() || secret.isEmpty()) {
            view.showError(EMPTY_FIELDS);
            return;
        }
        view.clearError();
        view.showBusy(true);
        executor.submit(() -> login.login(user, secret), this::onSuccess, this::onError);
    }

    private void onSuccess(AuthTokens tokens) {
        view.showBusy(false);
        view.clearPassword();
        onAuthenticated.accept(tokens);
    }

    private void onError(Exception error) {
        view.showBusy(false);
        view.clearPassword();
        view.showError(messageFor(error));
    }

    private static String messageFor(Exception error) {
        if (error instanceof ApiException api && ErrorCodes.ACCOUNT_LOCKED.equals(api.code())) {
            var until = Dates.timeFromIso(api.detail("lockedUntil"));
            return until == null ? LOCKED : LOCKED + " Vuelva a intentar después de las " + until + ".";
        }
        return ApiErrors.messageFor(error);
    }
}
