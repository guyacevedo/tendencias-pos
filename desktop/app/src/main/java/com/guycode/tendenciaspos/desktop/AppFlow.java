package com.guycode.tendenciaspos.desktop;

import com.guycode.tendenciaspos.contracts.identity.AuthTokens;
import com.guycode.tendenciaspos.contracts.identity.UserRole;
import com.guycode.tendenciaspos.desktop.api.ApiClient;
import com.guycode.tendenciaspos.desktop.api.AuthApi;
import com.guycode.tendenciaspos.desktop.api.ConnectionMonitor;
import com.guycode.tendenciaspos.desktop.api.SettingsApi;
import com.guycode.tendenciaspos.desktop.api.SystemApi;
import com.guycode.tendenciaspos.desktop.api.UsersApi;
import com.guycode.tendenciaspos.desktop.auth.SessionManager;
import com.guycode.tendenciaspos.desktop.core.ClientConfig;
import com.guycode.tendenciaspos.desktop.core.UiExecutor;
import com.guycode.tendenciaspos.desktop.home.HomePanel;
import com.guycode.tendenciaspos.desktop.home.HomePresenter;
import com.guycode.tendenciaspos.desktop.login.ChangePasswordPresenter;
import com.guycode.tendenciaspos.desktop.login.LoginFrame;
import com.guycode.tendenciaspos.desktop.login.LoginPresenter;
import com.guycode.tendenciaspos.desktop.settings.SettingsPanel;
import com.guycode.tendenciaspos.desktop.settings.SettingsPresenter;
import com.guycode.tendenciaspos.desktop.shell.MainFrame;
import com.guycode.tendenciaspos.desktop.shell.OfflineAware;
import com.guycode.tendenciaspos.desktop.shell.Route;
import com.guycode.tendenciaspos.desktop.users.UsersPanel;
import com.guycode.tendenciaspos.desktop.users.UsersPresenter;
import com.guycode.tendenciaspos.ui.Toast;
import java.awt.Window;
import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Hilo de la aplicación: ingreso → (cambio de clave obligatorio) → ventana principal, y de vuelta al
 * ingreso al cerrar sesión o cuando el servidor deja de aceptar la sesión. Todo ocurre en el EDT.
 */
final class AppFlow {
    private static final Duration RETRY_INTERVAL = Duration.ofSeconds(5);

    private final ClientConfig config;
    private final ConnectionMonitor monitor;
    private final ApiClient api;
    private final SystemApi systemApi;
    private final AuthApi authApi;
    private final UsersApi usersApi;
    private final SettingsApi settingsApi;
    private final SessionManager session;
    private final UiExecutor executor;

    private LoginFrame loginFrame;
    private MainFrame mainFrame;
    private Runnable onReconnect;

    AppFlow() {
        config = ClientConfig.load();
        monitor = new ConnectionMonitor(RETRY_INTERVAL);
        api = new ApiClient(config, monitor);
        systemApi = new SystemApi(api);
        authApi = new AuthApi(api);
        usersApi = new UsersApi(api);
        settingsApi = new SettingsApi(api);
        session = new SessionManager(authApi, Clock.systemUTC());
        api.setAuthorization(session);
        executor = new UiExecutor();

        monitor.setProbe(systemApi::version);
        monitor.addListener(online -> SwingUtilities.invokeLater(() -> onConnectionChanged(online)));
        session.onSessionEnded(message -> SwingUtilities.invokeLater(() -> showLogin(message)));
    }

    void start() {
        showLogin(null);
    }

    /** Vuelve al ingreso; {@code message} explica por qué, o es {@code null} al arrancar. */
    private void showLogin(String message) {
        onReconnect = null;
        close(mainFrame);
        mainFrame = null;
        close(loginFrame);

        var frame = new LoginFrame(RETRY_INTERVAL);
        loginFrame = frame;
        var login = new LoginPresenter(frame.loginPanel(), authApi::login, executor, this::onAuthenticated);
        frame.loginPanel().onSubmit(login::submit);
        var change = new ChangePasswordPresenter(
                frame.passwordPanel(), authApi::changePassword, executor, this::onPasswordChanged);
        frame.passwordPanel().onSubmit(change::submit);

        frame.setOffline(!monitor.isOnline());
        frame.setVisible(true);
        frame.showLogin(message);
    }

    private void onAuthenticated(AuthTokens tokens) {
        session.start(tokens);
        if (tokens.user().passwordChangeRequired()) {
            loginFrame.showPasswordChange(true);
        } else {
            openMain();
        }
    }

    /** Cambiar la clave devuelve una sesión nueva: reemplaza los tokens y entra. */
    private void onPasswordChanged(AuthTokens tokens) {
        session.start(tokens);
        openMain();
    }

    private void openMain() {
        var user = session.user();
        var screens = new EnumMap<Route, JComponent>(Route.class);

        var home = new HomePanel();
        var homePresenter = new HomePresenter(home, systemApi::version, executor, config.clientVersion());
        home.onRetry(homePresenter::load);
        screens.put(Route.HOME, home);

        UsersPresenter usersPresenter = null;
        SettingsPresenter settingsPresenter = null;
        if (user.roles().contains(UserRole.ADMIN)) {
            var usersPanel = new UsersPanel();
            usersPresenter = new UsersPresenter(usersPanel, usersApi, executor, user.id());
            wire(usersPanel, usersPresenter);
            screens.put(Route.USERS, usersPanel);

            var settingsPanel = new SettingsPanel();
            settingsPresenter = new SettingsPresenter(settingsPanel, settingsApi, executor);
            var presenter = settingsPresenter;
            settingsPanel.onSave(() -> presenter.save(
                    settingsPanel.storeNameValue(),
                    settingsPanel.taxIdValue(),
                    settingsPanel.addressValue(),
                    settingsPanel.phoneValue(),
                    settingsPanel.invoiceFormatValue()));
            screens.put(Route.SETTINGS, settingsPanel);
        }

        var frame = new MainFrame(screens, user, RETRY_INTERVAL, this::logout);
        frame.whenShown(Route.HOME, homePresenter::load);
        if (usersPresenter != null) {
            frame.whenShown(Route.USERS, usersPresenter::load);
        }
        if (settingsPresenter != null) {
            frame.whenShown(Route.SETTINGS, settingsPresenter::load);
        }
        mainFrame = frame;
        onReconnect = homePresenter::load;

        frame.setOffline(!monitor.isOnline());
        frame.setVisible(true);
        frame.navigate(Route.HOME);
        close(loginFrame);
        loginFrame = null;
    }

    private void wire(UsersPanel panel, UsersPresenter presenter) {
        panel.onSelect(presenter::select);
        panel.onNew(presenter::startNew);
        panel.onSave(() -> presenter.save(
                panel.usernameValue(), panel.fullNameValue(), panel.passwordValue(), panel.rolesValue()));
        panel.onResetPassword(() -> presenter.resetPassword(panel.passwordValue()));
        panel.onToggleActive(presenter::toggleActive);
    }

    private void logout() {
        executor.submit(
                () -> {
                    session.logout();
                    return null;
                },
                ignored -> showLogin(null),
                ignored -> showLogin(null));
    }

    private void onConnectionChanged(boolean online) {
        OfflineAware visible = mainFrame != null ? mainFrame : loginFrame;
        if (visible != null) {
            visible.setOffline(!online);
        }
        if (!online) {
            return;
        }
        Window anchor = mainFrame != null ? mainFrame : loginFrame;
        if (anchor != null) {
            Toast.success(anchor, "Conexión con el servidor restablecida.");
        }
        if (onReconnect != null) {
            onReconnect.run();
        }
    }

    private static void close(Window window) {
        if (window != null) {
            window.dispose();
        }
    }
}
