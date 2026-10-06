package com.guycode.tendenciaspos.desktop.shell;

import com.formdev.flatlaf.FlatClientProperties;
import com.guycode.tendenciaspos.ui.Icons;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/** Pantalla provisional para módulos que llegan en fases posteriores. */
final class PlaceholderPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    PlaceholderPanel(Route route) {
        super(new MigLayout("fill, wrap 1", "[center]", "push[]8[]4[]push"));
        add(new JLabel(Icons.of(route.icon(), 56, "Label.disabledForeground")));
        var title = new JLabel(route.title());
        title.putClientProperty(FlatClientProperties.STYLE_CLASS, "h2");
        add(title);
        var hint = new JLabel("Este módulo estará disponible en próximas versiones.");
        hint.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");
        add(hint);
    }
}
