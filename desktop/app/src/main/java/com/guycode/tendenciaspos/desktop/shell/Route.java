package com.guycode.tendenciaspos.desktop.shell;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;

/** Pantallas navegables desde el menú lateral, en el orden en que aparecen. */
public enum Route {
    HOME("Inicio", MaterialDesignH.HOME_OUTLINE),
    SALES("Ventas", MaterialDesignC.CASH_REGISTER),
    PRODUCTS("Productos", MaterialDesignS.SHOE_SNEAKER),
    INVENTORY("Inventario", MaterialDesignP.PACKAGE_VARIANT_CLOSED),
    CUSTOMERS("Clientes", MaterialDesignA.ACCOUNT_GROUP_OUTLINE),
    PURCHASES("Compras", MaterialDesignT.TRUCK_OUTLINE),
    CASH("Caja", MaterialDesignC.CASH_MULTIPLE),
    REPORTS("Reportes", MaterialDesignC.CHART_BOX_OUTLINE),
    SETTINGS("Configuración", MaterialDesignC.COG_OUTLINE);

    private final String title;
    private final Ikon icon;

    Route(String title, Ikon icon) {
        this.title = title;
        this.icon = icon;
    }

    public String title() {
        return title;
    }

    public Ikon icon() {
        return icon;
    }
}
