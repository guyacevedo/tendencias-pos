package com.guycode.tendenciaspos.desktop;

import com.guycode.tendenciaspos.desktop.api.ApiClient;
import com.guycode.tendenciaspos.desktop.api.ConnectionMonitor;
import com.guycode.tendenciaspos.desktop.api.SystemApi;
import com.guycode.tendenciaspos.desktop.core.ClientConfig;
import com.guycode.tendenciaspos.desktop.core.UiExecutor;
import com.guycode.tendenciaspos.desktop.home.HomePanel;
import com.guycode.tendenciaspos.desktop.home.HomePresenter;
import com.guycode.tendenciaspos.desktop.shell.MainFrame;
import com.guycode.tendenciaspos.desktop.shell.OfflineBar;
import com.guycode.tendenciaspos.desktop.shell.Route;
import com.guycode.tendenciaspos.ui.Theme;
import com.guycode.tendenciaspos.ui.Toast;
import java.time.Duration;
import java.util.Map;
import javax.swing.SwingUtilities;

/** Punto de entrada del escritorio: arma las dependencias y abre la ventana principal. */
public final class DesktopApp {
    private static final Duration RETRY_INTERVAL = Duration.ofSeconds(5);

    private DesktopApp() {}

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "Tendencias POS");
        var config = ClientConfig.load();
        var monitor = new ConnectionMonitor(RETRY_INTERVAL);
        var api = new ApiClient(config, monitor);
        var systemApi = new SystemApi(api);
        monitor.setProbe(systemApi::version);
        var executor = new UiExecutor();

        SwingUtilities.invokeLater(() -> {
            Theme.install();
            var home = new HomePanel();
            var homePresenter = new HomePresenter(home, systemApi::version, executor, config.clientVersion());
            home.onRetry(homePresenter::load);

            var offlineBar = new OfflineBar(RETRY_INTERVAL);
            var frame = new MainFrame(Map.of(Route.HOME, home), offlineBar);
            monitor.addListener(online -> SwingUtilities.invokeLater(() -> {
                offlineBar.setOffline(!online);
                if (online) {
                    Toast.success(frame, "Conexión con el servidor restablecida.");
                    homePresenter.load();
                }
            }));

            frame.navigate(Route.HOME);
            frame.setVisible(true);
            homePresenter.load();
        });
    }
}
