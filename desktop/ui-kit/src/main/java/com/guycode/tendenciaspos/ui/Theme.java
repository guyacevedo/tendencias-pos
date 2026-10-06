package com.guycode.tendenciaspos.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.prefs.Preferences;

/** Punto único para instalar el aspecto visual (FlatLaf + {@code TposTheme.properties}) y cambiar de modo. */
public final class Theme {
    /** Modo de color de la interfaz. */
    public enum Mode {
        LIGHT,
        DARK
    }

    private static final String RESOURCE = "TposTheme.properties";
    private static final String PREF_KEY = "theme.mode";
    private static volatile Mode current = Mode.LIGHT;

    private Theme() {}

    /** Instala la fuente Inter y el modo guardado en las preferencias del usuario (claro por defecto). */
    public static void install() {
        FlatInterFont.install();
        FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
        FlatLaf.setPreferredLightFontFamily(FlatInterFont.FAMILY_LIGHT);
        FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);
        apply(savedMode());
    }

    public static Mode current() {
        return current;
    }

    /** Alterna claro/oscuro con animación y recuerda la elección. Llamar en el EDT. */
    public static void toggle() {
        var next = current == Mode.LIGHT ? Mode.DARK : Mode.LIGHT;
        FlatAnimatedLafChange.showSnapshot();
        apply(next);
        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
        preferences().put(PREF_KEY, next.name());
    }

    static void apply(Mode mode) {
        FlatLaf laf = mode == Mode.DARK ? new FlatDarkLaf() : new FlatLightLaf();
        laf.setExtraDefaults(defaultsFor(mode));
        FlatLaf.setup(laf);
        current = mode;
    }

    /** Propiedades del tema para un modo: comunes + las de su prefijo, sin las del otro modo. */
    static Map<String, String> defaultsFor(Mode mode) {
        var own = mode == Mode.DARK ? "[dark]" : "[light]";
        Map<String, String> result = new TreeMap<>();
        for (var entry : load().entrySet()) {
            var key = entry.getKey().toString();
            var value = entry.getValue().toString().strip();
            if (key.startsWith(own)) {
                result.put(key.substring(own.length()), value);
            } else if (!key.startsWith("[")) {
                result.putIfAbsent(key, value);
            }
        }
        return result;
    }

    private static Properties load() {
        try (InputStream in = Theme.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("No se encontró " + RESOURCE);
            }
            var props = new Properties();
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Mode savedMode() {
        try {
            return Mode.valueOf(preferences().get(PREF_KEY, Mode.LIGHT.name()));
        } catch (IllegalArgumentException e) {
            return Mode.LIGHT;
        }
    }

    private static Preferences preferences() {
        return Preferences.userNodeForPackage(Theme.class);
    }
}
