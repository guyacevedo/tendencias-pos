package com.guycode.tendenciaspos.desktop.shell;

import com.formdev.flatlaf.FlatClientProperties;
import com.guycode.tendenciaspos.ui.Icons;
import java.time.Duration;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

/** Franja superior visible solo mientras no hay conexión con la API. */
public final class OfflineBar extends JPanel {
    private static final long serialVersionUID = 1L;

    public OfflineBar(Duration retryInterval) {
        super(new MigLayout("insets 6 12 6 12, gap 8, center", "", ""));
        putClientProperty(FlatClientProperties.STYLE, "background: $Tpos.dangerColor");
        var text = new JLabel("Sin conexión con el servidor. Reintentando cada " + retryInterval.toSeconds() + " s…");
        text.setIcon(Icons.of(MaterialDesignL.LAN_DISCONNECT, Icons.MEDIUM, "Tpos.onStatusColor"));
        text.setIconTextGap(8);
        text.putClientProperty(FlatClientProperties.STYLE, "foreground: $Tpos.onStatusColor; font: semibold");
        add(text);
        setVisible(false);
    }

    /** Llamar en el EDT. */
    public void setOffline(boolean offline) {
        setVisible(offline);
        revalidate();
    }
}
