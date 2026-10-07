package com.guycode.tendenciaspos.desktop.shell;

/** Ventana que puede mostrar que no hay conexión con la API. */
public interface OfflineAware {
    /** Llamar en el EDT. */
    void setOffline(boolean offline);
}
