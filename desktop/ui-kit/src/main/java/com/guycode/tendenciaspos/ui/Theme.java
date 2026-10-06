package com.guycode.tendenciaspos.ui;

import com.formdev.flatlaf.FlatLightLaf;

/** Punto único para instalar el aspecto visual de la aplicación. */
public final class Theme {
    private Theme() {}

    public static void install() {
        FlatLightLaf.setup();
    }
}
