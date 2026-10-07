package com.guycode.tendenciaspos.desktop;

import com.guycode.tendenciaspos.ui.Theme;
import javax.swing.SwingUtilities;

/** Punto de entrada del escritorio. */
public final class DesktopApp {
    private DesktopApp() {}

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "Tendencias POS");
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new AppFlow().start();
        });
    }
}
