package com.guycode.tendenciaspos.desktop;

import com.guycode.tendenciaspos.ui.Theme;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/** Punto de entrada del escritorio. La ventana real llega en la sesión 0.3. */
public final class DesktopApp {
    private DesktopApp() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            var frame = new JFrame("Tendencias POS");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.add(new JLabel("Tendencias POS 2.0", SwingConstants.CENTER));
            frame.setSize(1024, 640);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
