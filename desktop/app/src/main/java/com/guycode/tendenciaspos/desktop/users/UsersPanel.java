package com.guycode.tendenciaspos.desktop.users;

import com.formdev.flatlaf.FlatClientProperties;
import com.guycode.tendenciaspos.contracts.identity.UserResponse;
import com.guycode.tendenciaspos.contracts.identity.UserRole;
import com.guycode.tendenciaspos.desktop.core.Dates;
import com.guycode.tendenciaspos.ui.Fields;
import com.guycode.tendenciaspos.ui.Icons;
import com.guycode.tendenciaspos.ui.LoadingOverlay;
import com.guycode.tendenciaspos.ui.Toast;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongConsumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

/** Administración de usuarios: lista a la izquierda, formulario a la derecha. */
public final class UsersPanel extends JPanel implements UsersView {
    private static final long serialVersionUID = 1L;

    private final LoadingOverlay overlay = new LoadingOverlay();
    private final UsersTableModel model = new UsersTableModel();
    private final JTable table = new JTable(model);
    private final JTextField username = Fields.text("usuario");
    private final JTextField fullName = Fields.text("Nombre y apellido");
    private final JPasswordField password = Fields.password("Clave temporal");
    private final JCheckBox adminRole = new JCheckBox(UsersTableModel.label(UserRole.ADMIN));
    private final JCheckBox cashierRole = new JCheckBox(UsersTableModel.label(UserRole.CASHIER));
    private final JButton newUser = new JButton("Nuevo", Icons.of(MaterialDesignA.ACCOUNT_PLUS_OUTLINE, Icons.SMALL));
    private final JButton save = new JButton("Guardar", Icons.of(MaterialDesignC.CONTENT_SAVE_OUTLINE, Icons.SMALL));
    private final JButton resetPassword =
            new JButton("Restablecer clave", Icons.of(MaterialDesignL.LOCK_RESET, Icons.SMALL));
    private final JButton toggleActive = new JButton("Desactivar");
    private final JLabel state = new JLabel();
    private final JLabel error = new JLabel();
    private final Map<String, JLabel> fieldErrors = new LinkedHashMap<>();
    private boolean selecting;

    public UsersPanel() {
        super(new MigLayout(
                "fill, insets 24, gap 16", "[grow 60, fill, 320::][grow 40, fill, 300::420]", "[grow, fill]"));

        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        var scroll = new JScrollPane(table);
        scroll.putClientProperty(FlatClientProperties.STYLE, "arc: 12");

        var form = new JPanel(new MigLayout("wrap 2, insets 20, gap 10 6", "[90!][grow, fill, 150::]", ""));
        form.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: $Tpos.sidebarBackground");

        var title = new JLabel("Usuario");
        title.putClientProperty(FlatClientProperties.STYLE_CLASS, "h2");
        form.add(title, "span 2, gapbottom 8");

        addField(form, "Usuario", username, "username");
        addField(form, "Nombre", fullName, "fullName");

        var roles = new JPanel(new MigLayout("insets 0, gap 12", "", ""));
        roles.setOpaque(false);
        roles.add(adminRole);
        roles.add(cashierRole);
        form.add(caption("Roles"));
        form.add(roles);
        form.add(fieldError("roles"), "skip 1, hidemode 3");

        addField(form, "Clave", password, "password");

        state.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");
        form.add(state, "span 2, gaptop 4");

        error.setIcon(Icons.of(MaterialDesignA.ALERT_OUTLINE, Icons.SMALL, "Tpos.dangerColor"));
        error.setIconTextGap(8);
        error.putClientProperty(FlatClientProperties.STYLE, "foreground: $Tpos.dangerColor");
        error.setVisible(false);
        form.add(error, "span 2, gaptop 4, hidemode 3");

        var buttons = new JPanel(new MigLayout("wrap 2, insets 0, gap 6", "[grow, fill][grow, fill]", ""));
        buttons.setOpaque(false);
        buttons.add(save);
        buttons.add(newUser);
        buttons.add(resetPassword, "span 2");
        buttons.add(toggleActive, "span 2");
        form.add(buttons, "span 2, gaptop 12");

        add(overlay.wrap(scroll));
        add(form, "aligny top");
    }

    public void onSelect(LongConsumer action) {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || selecting) {
                return;
            }
            int row = table.getSelectedRow();
            if (row >= 0) {
                action.accept(model.at(table.convertRowIndexToModel(row)).id());
            }
        });
    }

    public void onNew(Runnable action) {
        newUser.addActionListener(e -> action.run());
    }

    public void onSave(Runnable action) {
        save.addActionListener(e -> action.run());
    }

    public void onResetPassword(Runnable action) {
        resetPassword.addActionListener(e -> action.run());
    }

    public void onToggleActive(Runnable action) {
        toggleActive.addActionListener(e -> action.run());
    }

    public String usernameValue() {
        return username.getText();
    }

    public String fullNameValue() {
        return fullName.getText();
    }

    public char[] passwordValue() {
        return password.getPassword();
    }

    public Set<UserRole> rolesValue() {
        var roles = EnumSet.noneOf(UserRole.class);
        if (adminRole.isSelected()) {
            roles.add(UserRole.ADMIN);
        }
        if (cashierRole.isSelected()) {
            roles.add(UserRole.CASHIER);
        }
        return roles;
    }

    @Override
    public void showLoading(boolean loading) {
        overlay.setActive(loading);
        if (loading) {
            error.setVisible(false);
        }
    }

    @Override
    public void showUsers(List<UserResponse> users) {
        selecting = true;
        model.setUsers(users);
        selecting = false;
    }

    @Override
    public void showForm(UserResponse user) {
        selecting = true;
        if (user == null) {
            table.clearSelection();
            username.setText("");
            fullName.setText("");
            adminRole.setSelected(false);
            cashierRole.setSelected(true);
            state.setText("Deberá cambiar la clave en su primer ingreso.");
        } else {
            int row = model.rowOf(user.id());
            if (row >= 0) {
                int viewRow = table.convertRowIndexToView(row);
                table.setRowSelectionInterval(viewRow, viewRow);
            }
            username.setText(user.username());
            fullName.setText(user.fullName());
            adminRole.setSelected(user.roles().contains(UserRole.ADMIN));
            cashierRole.setSelected(user.roles().contains(UserRole.CASHIER));
            state.setText(stateOf(user));
        }
        selecting = false;
        password.setText("");
        boolean creating = user == null;
        username.setEditable(creating);
        password.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT, creating ? "Clave temporal" : "Clave nueva (opcional)");
        resetPassword.setVisible(!creating);
        toggleActive.setVisible(!creating);
        toggleActive.setText(creating || user.active() ? "Desactivar" : "Activar");
        revalidate();
    }

    @Override
    public void showFieldErrors(Map<String, String> errors) {
        fieldErrors.forEach((field, label) -> {
            var message = errors.get(field);
            label.setText(message == null ? "" : message);
            label.setVisible(message != null);
        });
        Fields.markInvalid(username, errors.containsKey("username"));
        Fields.markInvalid(fullName, errors.containsKey("fullName"));
        Fields.markInvalid(password, errors.containsKey("password"));
        revalidate();
    }

    @Override
    public void showError(String message) {
        error.setText(message);
        error.setVisible(true);
        revalidate();
    }

    @Override
    public void showMessage(String message) {
        Toast.success(this, message);
    }

    private static String stateOf(UserResponse user) {
        var text = new StringBuilder(user.active() ? "Activo." : "Inactivo.");
        if (user.lockedUntil() != null) {
            text.append(" Bloqueado hasta las ")
                    .append(Dates.time(user.lockedUntil()))
                    .append('.');
        }
        if (user.passwordChangeRequired()) {
            text.append(" Debe cambiar la clave al ingresar.");
        }
        return text.toString();
    }

    private void addField(JPanel form, String label, JComponent field, String name) {
        form.add(caption(label));
        form.add(field);
        form.add(fieldError(name), "skip 1, hidemode 3");
    }

    private JLabel fieldError(String name) {
        var message = new JLabel();
        message.putClientProperty(FlatClientProperties.STYLE, "foreground: $Tpos.dangerColor");
        message.setVisible(false);
        fieldErrors.put(name, message);
        return message;
    }

    private static JComponent caption(String text) {
        var label = new JLabel(text);
        label.putClientProperty(FlatClientProperties.STYLE, "font: semibold");
        return label;
    }
}
