package com.guycode.tendenciaspos.desktop.users;

import com.guycode.tendenciaspos.contracts.identity.UserResponse;
import com.guycode.tendenciaspos.contracts.identity.UserRole;
import com.guycode.tendenciaspos.desktop.api.ApiException;
import com.guycode.tendenciaspos.desktop.api.UserGateway;
import com.guycode.tendenciaspos.desktop.core.ApiErrors;
import com.guycode.tendenciaspos.desktop.core.ErrorCodes;
import com.guycode.tendenciaspos.desktop.core.UiExecutor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Lista y formulario de usuarios. Los usuarios no se borran: se desactivan. */
public final class UsersPresenter {
    static final int PASSWORD_MIN = 10;
    static final String PASSWORD_RESET = "Clave restablecida: el usuario deberá cambiarla al ingresar.";
    static final String SELF_DEACTIVATION = "No puede desactivar su propio usuario.";

    private final UsersView view;
    private final UserGateway gateway;
    private final UiExecutor executor;
    private final long currentUserId;

    private List<UserResponse> users = List.of();
    private UserResponse selected;

    public UsersPresenter(UsersView view, UserGateway gateway, UiExecutor executor, long currentUserId) {
        this.view = view;
        this.gateway = gateway;
        this.executor = executor;
        this.currentUserId = currentUserId;
    }

    public void load() {
        view.showLoading(true);
        executor.submit(gateway::list, this::onLoaded, this::onError);
    }

    /** Usuario seleccionado en la lista, o {@code null} si se está creando uno. */
    public UserResponse selected() {
        return selected;
    }

    public void startNew() {
        selected = null;
        view.showFieldErrors(Map.of());
        view.showForm(null);
    }

    public void select(long id) {
        users.stream().filter(u -> u.id() == id).findFirst().ifPresent(user -> {
            selected = user;
            view.showFieldErrors(Map.of());
            view.showForm(user);
        });
    }

    /** Crea el usuario si no hay uno seleccionado; si lo hay, actualiza nombre y roles. */
    public void save(String username, String fullName, char[] password, Set<UserRole> roles) {
        var secret = text(password);
        var errors = validate(username, fullName, secret, roles, selected == null);
        if (!errors.isEmpty()) {
            view.showFieldErrors(errors);
            return;
        }
        view.showFieldErrors(Map.of());
        view.showLoading(true);
        var name = fullName.strip();
        if (selected == null) {
            var user = username.strip();
            executor.submit(
                    () -> gateway.create(user, name, secret, roles),
                    saved -> afterSave(saved, "Usuario creado."),
                    this::onError);
        } else {
            long id = selected.id();
            executor.submit(
                    () -> gateway.update(id, name, roles),
                    saved -> afterSave(saved, "Usuario actualizado."),
                    this::onError);
        }
    }

    public void resetPassword(char[] newPassword) {
        if (selected == null) {
            return;
        }
        var secret = text(newPassword);
        if (secret.length() < PASSWORD_MIN) {
            view.showFieldErrors(Map.of("password", "Mínimo " + PASSWORD_MIN + " caracteres."));
            return;
        }
        view.showFieldErrors(Map.of());
        view.showLoading(true);
        long id = selected.id();
        executor.submit(
                () -> {
                    gateway.resetPassword(id, secret);
                    return gateway.list();
                },
                list -> {
                    onLoaded(list);
                    select(id);
                    view.showMessage(PASSWORD_RESET);
                },
                this::onError);
    }

    /** Activa o desactiva el usuario seleccionado. */
    public void toggleActive() {
        if (selected == null) {
            return;
        }
        if (selected.active() && selected.id() == currentUserId) {
            view.showError(SELF_DEACTIVATION);
            return;
        }
        view.showLoading(true);
        long id = selected.id();
        boolean activate = !selected.active();
        executor.submit(
                () -> activate ? gateway.activate(id) : gateway.deactivate(id),
                saved -> afterSave(saved, activate ? "Usuario activado." : "Usuario desactivado."),
                this::onError);
    }

    private void afterSave(UserResponse saved, String message) {
        view.showLoading(false);
        view.showMessage(message);
        selected = saved;
        view.showForm(saved);
        executor.submit(gateway::list, this::onLoaded, this::onError);
    }

    private void onLoaded(List<UserResponse> list) {
        view.showLoading(false);
        users = List.copyOf(list);
        view.showUsers(users);
        if (selected != null) {
            users.stream().filter(u -> u.id() == selected.id()).findFirst().ifPresent(u -> {
                selected = u;
                view.showForm(u);
            });
        }
    }

    private void onError(Exception error) {
        view.showLoading(false);
        if (error instanceof ApiException api && ErrorCodes.VALIDATION_FAILED.equals(api.code())) {
            var errors = api.fieldErrors();
            if (!errors.isEmpty()) {
                view.showFieldErrors(errors);
                return;
            }
        }
        view.showError(ApiErrors.messageFor(error));
    }

    private static Map<String, String> validate(
            String username, String fullName, String password, Set<UserRole> roles, boolean creating) {
        var errors = new LinkedHashMap<String, String>();
        if (creating && (username == null || username.strip().isEmpty())) {
            errors.put("username", "El usuario es obligatorio.");
        }
        if (fullName == null || fullName.strip().isEmpty()) {
            errors.put("fullName", "El nombre es obligatorio.");
        }
        if (creating && password.length() < PASSWORD_MIN) {
            errors.put("password", "Mínimo " + PASSWORD_MIN + " caracteres.");
        }
        if (roles == null || roles.isEmpty()) {
            errors.put("roles", "Asigne al menos un rol.");
        }
        return errors;
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
