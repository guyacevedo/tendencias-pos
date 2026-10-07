package com.guycode.tendenciaspos.desktop.shell;

import com.formdev.flatlaf.FlatClientProperties;
import com.guycode.tendenciaspos.ui.Icons;
import com.guycode.tendenciaspos.ui.Theme;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

/** Menú lateral con las rutas que ve el usuario. Contraído muestra solo iconos (con tooltip). */
final class SideMenu extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String EXPANDED_COLUMN = "[200!]";
    private static final String COLLAPSED_COLUMN = "[44!]";

    private final Map<Route, JToggleButton> items = new EnumMap<>(Route.class);
    private final JButton themeButton = new JButton();
    private final JButton logoutButton = new JButton();
    private final JLabel userLabel = new JLabel();
    private final MigLayout layout = new MigLayout("wrap 1, insets 8, gap 2", EXPANDED_COLUMN, "");
    private final String userName;
    private boolean collapsed;

    SideMenu(Collection<Route> routes, String userName, Consumer<Route> onNavigate, Runnable onLogout) {
        setLayout(layout);
        putClientProperty(FlatClientProperties.STYLE, "background: $Tpos.sidebarBackground");
        this.userName = userName;

        var toggle = new JButton(Icons.of(MaterialDesignM.MENU));
        toggle.setToolTipText("Mostrar u ocultar nombres del menú");
        borderless(toggle);
        toggle.addActionListener(e -> setCollapsed(!collapsed));
        add(toggle, "h 40!, gapbottom 8");

        var group = new ButtonGroup();
        for (var route : routes) {
            var item = new JToggleButton(route.title(), Icons.of(route.icon()));
            item.setToolTipText(route.title());
            borderless(item);
            item.addActionListener(e -> onNavigate.accept(route));
            group.add(item);
            items.put(route, item);
            add(item, "growx, h 40!");
        }

        userLabel.setIcon(Icons.of(MaterialDesignA.ACCOUNT_CIRCLE_OUTLINE, Icons.MEDIUM));
        userLabel.setIconTextGap(12);
        userLabel.setToolTipText(userName);
        add(new JSeparator(), "growx, pushy, aligny bottom, gaptop 8");
        add(userLabel, "growx, h 32!");

        logoutButton.setIcon(Icons.of(MaterialDesignL.LOGOUT));
        borderless(logoutButton);
        logoutButton.addActionListener(e -> onLogout.run());
        add(logoutButton, "growx, h 40!");

        borderless(themeButton);
        themeButton.addActionListener(e -> {
            Theme.toggle();
            refreshLabels();
        });
        add(themeButton, "growx, h 40!");
        refreshLabels();
    }

    void select(Route route) {
        var item = items.get(route);
        if (item != null) {
            item.setSelected(true);
        }
    }

    boolean isCollapsed() {
        return collapsed;
    }

    void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        layout.setColumnConstraints(collapsed ? COLLAPSED_COLUMN : EXPANDED_COLUMN);
        items.forEach((route, item) -> applyLabel(item, route.title()));
        refreshLabels();
        revalidate();
        repaint();
    }

    private void refreshLabels() {
        boolean dark = Theme.current() == Theme.Mode.DARK;
        themeButton.setIcon(Icons.of(dark ? MaterialDesignW.WHITE_BALANCE_SUNNY : MaterialDesignW.WEATHER_NIGHT));
        var themeLabel = dark ? "Modo claro" : "Modo oscuro";
        themeButton.setToolTipText(themeLabel);
        applyLabel(themeButton, themeLabel);
        logoutButton.setToolTipText("Cerrar sesión");
        applyLabel(logoutButton, "Cerrar sesión");
        userLabel.setText(collapsed ? "" : userName);
        userLabel.setHorizontalAlignment(collapsed ? SwingConstants.CENTER : SwingConstants.LEADING);
    }

    private void applyLabel(AbstractButton button, String label) {
        button.setText(collapsed ? "" : label);
        button.setHorizontalAlignment(collapsed ? SwingConstants.CENTER : SwingConstants.LEADING);
    }

    private static void borderless(AbstractButton button) {
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        button.setHorizontalAlignment(SwingConstants.LEADING);
        button.setIconTextGap(12);
        button.setFocusable(false);
    }
}
