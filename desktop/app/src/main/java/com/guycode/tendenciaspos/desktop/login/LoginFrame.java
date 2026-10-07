package com.guycode.tendenciaspos.desktop.login;

import com.guycode.tendenciaspos.desktop.shell.OfflineAware;
import com.guycode.tendenciaspos.desktop.shell.OfflineBar;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.time.Duration;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

/** Ventana previa a la aplicación: ingreso y, si hace falta, cambio de clave obligatorio. */
public final class LoginFrame extends JFrame implements OfflineAware {
    private static final long serialVersionUID = 1L;

    private static final String LOGIN_CARD = "login";
    private static final String PASSWORD_CARD = "password";

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final OfflineBar offlineBar;
    private final LoginPanel loginPanel = new LoginPanel();
    private final ChangePasswordPanel passwordPanel = new ChangePasswordPanel();

    public LoginFrame(Duration retryInterval) {
        super("Tendencias POS");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        content.add(loginPanel, LOGIN_CARD);
        content.add(passwordPanel, PASSWORD_CARD);
        this.offlineBar = new OfflineBar(retryInterval);

        var root = new JPanel(new MigLayout("fill, insets 0, gap 0", "[grow, fill]", "[][grow, fill]"));
        root.add(offlineBar, "growx, wrap, hidemode 3");
        root.add(content, "grow");
        setContentPane(root);

        setMinimumSize(new Dimension(460, 520));
        setSize(520, 620);
        setLocationRelativeTo(null);
    }

    public LoginPanel loginPanel() {
        return loginPanel;
    }

    public ChangePasswordPanel passwordPanel() {
        return passwordPanel;
    }

    /** Muestra el ingreso; {@code message} explica por qué se volvió aquí, o es {@code null}. */
    public void showLogin(String message) {
        cards.show(content, LOGIN_CARD);
        getRootPane().setDefaultButton(loginPanel.submitButton());
        if (message == null) {
            loginPanel.clearError();
        } else {
            loginPanel.showError(message);
        }
        loginPanel.focusFirstField();
    }

    public void showPasswordChange(boolean mandatory) {
        passwordPanel.setMandatory(mandatory);
        passwordPanel.clearError();
        cards.show(content, PASSWORD_CARD);
        getRootPane().setDefaultButton(passwordPanel.submitButton());
        passwordPanel.focusFirstField();
    }

    @Override
    public void setOffline(boolean offline) {
        offlineBar.setOffline(offline);
    }
}
