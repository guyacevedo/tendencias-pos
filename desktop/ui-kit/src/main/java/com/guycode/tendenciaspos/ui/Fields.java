package com.guycode.tendenciaspos.ui;

import com.formdev.flatlaf.FlatClientProperties;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** Campos de formulario con el estilo del tema: texto de ayuda y marca de error. */
public final class Fields {
    private Fields() {}

    public static JTextField text(String placeholder) {
        var field = new JTextField();
        placeholder(field, placeholder);
        return field;
    }

    /** Campo de clave con el botón de mostrar u ocultar que trae el tema. */
    public static JPasswordField password(String placeholder) {
        var field = new JPasswordField();
        placeholder(field, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
        return field;
    }

    /** Marca el campo con el borde de error (o lo devuelve a la normalidad). */
    public static void markInvalid(JComponent field, boolean invalid) {
        field.putClientProperty(FlatClientProperties.OUTLINE, invalid ? FlatClientProperties.OUTLINE_ERROR : null);
    }

    private static void placeholder(JComponent field, String placeholder) {
        if (placeholder != null && !placeholder.isBlank()) {
            field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        }
    }
}
