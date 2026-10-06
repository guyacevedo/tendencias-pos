package com.guycode.tendenciaspos.desktop.shell;

import com.formdev.flatlaf.FlatClientProperties;
import com.guycode.tendenciaspos.ui.Icons;
import com.guycode.tendenciaspos.ui.Theme;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

/** Menú lateral con una entrada por {@link Route}. Contraído muestra solo iconos (con tooltip). */
final class SideMenu extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String EXPANDED_COLUMN = "[200!]";
    private static final String COLLAPSED_COLUMN = "[44!]";

    private final Map<Route, JToggleButton> items = new EnumMap<>(Route.class);
    private final JButton themeButton = new JButton();
    private final MigLayout layout = new MigLayout("wrap 1, insets 8, gap 2", EXPANDED_COLUMN, "");
    private boolean collapsed;

    SideMenu(Consumer<Route> onNavigate) {
        setLayout(layout);
        putClientProperty(FlatClientProperties.STYLE, "background: $Tpos.sidebarBackground");

        var toggle = new JButton(Icons.of(MaterialDesignM.MENU));
        toggle.setToolTipText("Mostrar u ocultar nombres del menú");
        borderless(toggle);
        toggle.addActionListener(e -> setCollapsed(!collapsed));
        add(toggle, "h 40!, gapbottom 8");

        var group = new ButtonGroup();
        for (var route : Route.values()) {
            var item = new JToggleButton(route.title(), Icons.of(route.icon()));
            item.setToolTipText(route.title());
            borderless(item);
            item.addActionListener(e -> onNavigate.accept(route));
            group.add(item);
            items.put(route, item);
            add(item, "growx, h 40!");
        }

        borderless(themeButton);
        themeButton.addActionListener(e -> {
            Theme.toggle();
            refreshThemeButton();
        });
        add(themeButton, "growx, h 40!, pushy, aligny bottom");
        refreshThemeButton();
    }

    void select(Route route) {
        items.get(route).setSelected(true);
    }

    boolean isCollapsed() {
        return collapsed;
    }

    void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        layout.setColumnConstraints(collapsed ? COLLAPSED_COLUMN : EXPANDED_COLUMN);
        items.forEach((route, item) -> applyLabel(item, route.title()));
        refreshThemeButton();
        revalidate();
        repaint();
    }

    private void refreshThemeButton() {
        boolean dark = Theme.current() == Theme.Mode.DARK;
        themeButton.setIcon(Icons.of(dark ? MaterialDesignW.WHITE_BALANCE_SUNNY : MaterialDesignW.WEATHER_NIGHT));
        var label = dark ? "Modo claro" : "Modo oscuro";
        themeButton.setToolTipText(label);
        applyLabel(themeButton, label);
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
