package com.guycode.tendenciaspos.desktop.login;

import com.formdev.flatlaf.FlatClientProperties;
import com.guycode.tendenciaspos.ui.Fields;
import com.guycode.tendenciaspos.ui.Icons;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

/** Cambio de la clave propia; también es la pantalla obligatoria del primer ingreso. */
public final class ChangePasswordPanel extends JPanel implements ChangePasswordView {
    private static final long serialVersionUID = 1L;

    /** Envío del formulario con lo escrito en los tres campos. */
    public interface SubmitAction {
        void submit(char[] current, char[] next, char[] confirmation);
    }

    private final JPasswordField current = Fields.password("Clave actual");
    private final JPasswordField next = Fields.password("Clave nueva");
    private final JPasswordField confirmation = Fields.password("Repita la clave nueva");
    private final JButton submit = new JButton("Guardar clave");
    private final JLabel hint = new JLabel();
    private final JLabel error = new JLabel();

    public ChangePasswordPanel() {
        super(new MigLayout("fill, insets 24", "[center]", "[center]"));

        var card = new JPanel(new MigLayout("wrap 1, insets 32, gap 6", "[grow, fill, 320::]", ""));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: $Tpos.sidebarBackground");

        card.add(new JLabel(Icons.of(MaterialDesignL.LOCK_RESET, Icons.LARGE)), "align center, gapbottom 4");
        var title = new JLabel("Cambiar la clave");
        title.putClientProperty(FlatClientProperties.STYLE_CLASS, "h2");
        card.add(title, "align center");

        hint.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");
        card.add(hint, "align center, gapbottom 16");

        card.add(current, "gapbottom 4");
        card.add(next, "gapbottom 4");
        card.add(confirmation, "gapbottom 4");

        error.setIcon(Icons.of(MaterialDesignA.ALERT_OUTLINE, Icons.SMALL, "Tpos.dangerColor"));
        error.setIconTextGap(8);
        error.putClientProperty(FlatClientProperties.STYLE, "foreground: $Tpos.dangerColor");
        error.setVisible(false);
        card.add(error, "hidemode 3");

        card.add(submit, "h 36!, gaptop 8");

        setMandatory(false);
        add(card, "align center");
    }

    public void onSubmit(SubmitAction action) {
        Runnable send = () -> action.submit(current.getPassword(), next.getPassword(), confirmation.getPassword());
        submit.addActionListener(e -> send.run());
        current.addActionListener(e -> next.requestFocusInWindow());
        next.addActionListener(e -> confirmation.requestFocusInWindow());
        confirmation.addActionListener(e -> send.run());
    }

    /** En el primer ingreso (o tras un restablecimiento) el cambio es obligatorio. */
    public void setMandatory(boolean mandatory) {
        hint.setText(
                mandatory
                        ? "Debe cambiar su clave antes de continuar."
                        : "Mínimo " + ChangePasswordPresenter.MIN_LENGTH + " caracteres.");
    }

    public void focusFirstField() {
        current.requestFocusInWindow();
    }

    public JButton submitButton() {
        return submit;
    }

    @Override
    public void showBusy(boolean busy) {
        current.setEnabled(!busy);
        next.setEnabled(!busy);
        confirmation.setEnabled(!busy);
        submit.setEnabled(!busy);
        submit.setText(busy ? "Guardando…" : "Guardar clave");
    }

    @Override
    public void showError(String message) {
        error.setText(message);
        error.setVisible(true);
        revalidate();
    }

    @Override
    public void clearError() {
        error.setVisible(false);
    }

    @Override
    public void clearFields() {
        current.setText("");
        next.setText("");
        confirmation.setText("");
    }
}
