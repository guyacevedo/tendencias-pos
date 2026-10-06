package com.guycode.tendenciaspos.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;

/** Aviso breve no modal en la esquina inferior derecha de la ventana. Se cierra solo o con un clic. */
public final class Toast {
    /** Tipo de aviso: define color e icono. */
    public enum Kind {
        INFO("Tpos.infoColor", MaterialDesignI.INFORMATION_OUTLINE),
        SUCCESS("Tpos.successColor", MaterialDesignC.CHECK_CIRCLE_OUTLINE),
        ERROR("Tpos.dangerColor", MaterialDesignA.ALERT_CIRCLE_OUTLINE);

        private final String colorKey;
        private final Ikon ikon;

        Kind(String colorKey, Ikon ikon) {
            this.colorKey = colorKey;
            this.ikon = ikon;
        }
    }

    private static final int DURATION_MS = 3500;
    private static final int MARGIN = 24;
    private static JWindow current;

    private Toast() {}

    public static void info(Component anchor, String message) {
        show(anchor, Kind.INFO, message);
    }

    public static void success(Component anchor, String message) {
        show(anchor, Kind.SUCCESS, message);
    }

    public static void error(Component anchor, String message) {
        show(anchor, Kind.ERROR, message);
    }

    /** Muestra el aviso (reemplaza al anterior). Llamar en el EDT. */
    public static void show(Component anchor, Kind kind, String message) {
        Window owner = anchor instanceof Window w ? w : SwingUtilities.getWindowAncestor(anchor);
        dismiss();

        var panel = new JPanel(new MigLayout("insets 10 14 10 14, gap 8", "[][grow]", "[]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "background: $" + kind.colorKey);
        panel.add(new JLabel(Icons.of(kind.ikon, Icons.MEDIUM, "Tpos.onStatusColor")));
        var text = new JLabel(message);
        text.putClientProperty(FlatClientProperties.STYLE, "foreground: $Tpos.onStatusColor");
        panel.add(text);

        var window = new JWindow(owner);
        window.setFocusableWindowState(false);
        window.setContentPane(panel);
        window.pack();
        if (owner != null && owner.isShowing()) {
            window.setLocation(
                    owner.getX() + owner.getWidth() - window.getWidth() - MARGIN,
                    owner.getY() + owner.getHeight() - window.getHeight() - MARGIN);
        } else {
            window.setLocationRelativeTo(null);
        }
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                close(window);
            }
        });
        current = window;
        window.setVisible(true);

        var timer = new Timer(DURATION_MS, e -> close(window));
        timer.setRepeats(false);
        timer.start();
    }

    private static void dismiss() {
        if (current != null) {
            close(current);
        }
    }

    private static void close(JWindow window) {
        window.dispose();
        if (current == window) {
            current = null;
        }
    }
}
