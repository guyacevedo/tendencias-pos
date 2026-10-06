package com.guycode.tendenciaspos.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;
import javax.swing.UIManager;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Iconos Material Design (Ikonli MDI2) que toman su color del tema al pintarse, así siguen al modo
 * claro/oscuro sin recrearse.
 */
public final class Icons {
    public static final int SMALL = 16;
    public static final int MEDIUM = 20;
    public static final int LARGE = 32;

    private static final String DEFAULT_COLOR_KEY = "Label.foreground";

    private Icons() {}

    public static Icon of(Ikon ikon) {
        return of(ikon, MEDIUM);
    }

    public static Icon of(Ikon ikon, int size) {
        return of(ikon, size, DEFAULT_COLOR_KEY);
    }

    /** Icono cuyo color es la clave {@code colorKey} de {@link UIManager} (p. ej. {@code Tpos.dangerColor}). */
    public static Icon of(Ikon ikon, int size, String colorKey) {
        return new ThemedIcon(FontIcon.of(ikon, size), colorKey);
    }

    private record ThemedIcon(FontIcon icon, String colorKey) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Color color = c != null && !c.isEnabled()
                    ? UIManager.getColor("Label.disabledForeground")
                    : UIManager.getColor(colorKey);
            if (color != null) {
                icon.setIconColor(color);
            }
            icon.paintIcon(c, g, x, y);
        }

        @Override
        public int getIconWidth() {
            return icon.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return icon.getIconHeight();
        }
    }
}
