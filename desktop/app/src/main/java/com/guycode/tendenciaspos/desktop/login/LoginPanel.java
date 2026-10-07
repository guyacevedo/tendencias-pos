package com.guycode.tendenciaspos.desktop.login;

import com.formdev.flatlaf.FlatClientProperties;
import com.guycode.tendenciaspos.ui.Fields;
import com.guycode.tendenciaspos.ui.Icons;
import java.util.function.BiConsumer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

/** Ingreso: usuario, clave con botón de mostrar u ocultar, y Enter para enviar. */
public final class LoginPanel extends JPanel implements LoginView {
    private static final long serialVersionUID = 1L;

    private final JTextField username = Fields.text("Usuario");
    private final JPasswordField password = Fields.password("Clave");
    private final JButton submit = new JButton("Ingresar");
    private final JLabel error = new JLabel();

    public LoginPanel() {
        super(new MigLayout("fill, insets 24", "[center]", "[center]"));

        var card = new JPanel(new MigLayout("wrap 1, insets 32, gap 6", "[grow, fill, 300::]", ""));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: $Tpos.sidebarBackground");

        card.add(new JLabel(Icons.of(MaterialDesignS.SHOE_SNEAKER, Icons.LARGE)), "align center, gapbottom 4");
        var title = new JLabel("Tendencias POS");
        title.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");
        card.add(title, "align center");
        var subtitle = new JLabel("Inicie sesión para continuar");
        subtitle.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");
        card.add(subtitle, "align center, gapbottom 16");

        card.add(caption("Usuario"));
        card.add(username, "gapbottom 8");
        card.add(caption("Clave"));
        card.add(password, "gapbottom 8");

        error.setIcon(Icons.of(MaterialDesignA.ALERT_OUTLINE, Icons.SMALL, "Tpos.dangerColor"));
        error.setIconTextGap(8);
        error.putClientProperty(FlatClientProperties.STYLE, "foreground: $Tpos.dangerColor");
        error.setVisible(false);
        card.add(error, "hidemode 3, gapbottom 4");

        card.add(submit, "h 36!, gaptop 8");

        add(card, "align center");
    }

    /** Registra el envío del formulario (botón o Enter en cualquiera de los campos). */
    public void onSubmit(BiConsumer<String, char[]> action) {
        submit.addActionListener(e -> action.accept(username.getText(), password.getPassword()));
        username.addActionListener(e -> action.accept(username.getText(), password.getPassword()));
        password.addActionListener(e -> action.accept(username.getText(), password.getPassword()));
    }

    /** Deja el cursor en el primer campo vacío. Llamar en el EDT. */
    public void focusFirstField() {
        if (username.getText().isBlank()) {
            username.requestFocusInWindow();
        } else {
            password.requestFocusInWindow();
        }
    }

    public JButton submitButton() {
        return submit;
    }

    @Override
    public void showBusy(boolean busy) {
        username.setEnabled(!busy);
        password.setEnabled(!busy);
        submit.setEnabled(!busy);
        submit.setText(busy ? "Ingresando…" : "Ingresar");
    }

    @Override
    public void showError(String message) {
        error.setText(message);
        error.setVisible(true);
        Fields.markInvalid(password, true);
        revalidate();
    }

    @Override
    public void clearError() {
        error.setVisible(false);
        Fields.markInvalid(password, false);
    }

    @Override
    public void clearPassword() {
        password.setText("");
    }

    private static JComponent caption(String text) {
        var label = new JLabel(text);
        label.putClientProperty(FlatClientProperties.STYLE, "font: semibold");
        return label;
    }
}
