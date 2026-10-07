package com.guycode.tendenciaspos.desktop.login;

import com.guycode.tendenciaspos.contracts.identity.AuthTokens;
import com.guycode.tendenciaspos.desktop.core.ApiErrors;
import com.guycode.tendenciaspos.desktop.core.UiExecutor;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Cambio de la clave propia. El servidor exige entre 10 y 128 caracteres y devuelve una sesión nueva,
 * así que al terminar hay que reemplazar los tokens de la sesión.
 */
public final class ChangePasswordPresenter {
    static final int MIN_LENGTH = 10;
    static final String EMPTY_FIELDS = "Complete los tres campos.";
    static final String TOO_SHORT = "La clave nueva debe tener al menos " + MIN_LENGTH + " caracteres.";
    static final String MISMATCH = "La clave nueva y su confirmación no coinciden.";
    static final String SAME_PASSWORD = "La clave nueva debe ser distinta de la actual.";

    /** Cambio de clave contra la API; se ejecuta fuera del EDT. */
    public interface ChangePassword {
        AuthTokens change(String currentPassword, String newPassword);
    }

    private final ChangePasswordView view;
    private final ChangePassword changePassword;
    private final UiExecutor executor;
    private final Consumer<AuthTokens> onChanged;

    public ChangePasswordPresenter(
            ChangePasswordView view,
            ChangePassword changePassword,
            UiExecutor executor,
            Consumer<AuthTokens> onChanged) {
        this.view = view;
        this.changePassword = changePassword;
        this.executor = executor;
        this.onChanged = onChanged;
    }

    /** Las claves se borran de los arreglos en cuanto se arma la petición. */
    public void submit(char[] current, char[] next, char[] confirmation) {
        var currentText = text(current);
        var nextText = text(next);
        var confirmationText = text(confirmation);
        var error = validate(currentText, nextText, confirmationText);
        if (error != null) {
            view.showError(error);
            return;
        }
        view.clearError();
        view.showBusy(true);
        executor.submit(() -> changePassword.change(currentText, nextText), this::onSuccess, this::onError);
    }

    private static String validate(String current, String next, String confirmation) {
        if (current.isEmpty() || next.isEmpty() || confirmation.isEmpty()) {
            return EMPTY_FIELDS;
        }
        if (next.length() < MIN_LENGTH) {
            return TOO_SHORT;
        }
        if (!next.equals(confirmation)) {
            return MISMATCH;
        }
        if (next.equals(current)) {
            return SAME_PASSWORD;
        }
        return null;
    }

    private void onSuccess(AuthTokens tokens) {
        view.showBusy(false);
        view.clearFields();
        onChanged.accept(tokens);
    }

    private void onError(Exception error) {
        view.showBusy(false);
        view.clearFields();
        view.showError(ApiErrors.messageFor(error));
    }

    private static String text(char[] value) {
        if (value == null) {
            return "";
        }
        var result = new String(value);
        Arrays.fill(value, '\0');
        return result;
    }
}
