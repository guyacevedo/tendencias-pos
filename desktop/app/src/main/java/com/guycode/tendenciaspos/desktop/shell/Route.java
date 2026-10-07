package com.guycode.tendenciaspos.desktop.shell;

import com.guycode.tendenciaspos.contracts.identity.UserRole;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;

/** Pantallas navegables desde el menú lateral, en el orden en que aparecen y con quién las ve. */
public enum Route {
    HOME("Inicio", MaterialDesignH.HOME_OUTLINE, UserRole.ADMIN, UserRole.CASHIER),
    SALES("Ventas", MaterialDesignC.CASH_REGISTER, UserRole.ADMIN, UserRole.CASHIER),
    PRODUCTS("Productos", MaterialDesignS.SHOE_SNEAKER, UserRole.ADMIN),
    INVENTORY("Inventario", MaterialDesignP.PACKAGE_VARIANT_CLOSED, UserRole.ADMIN),
    CUSTOMERS("Clientes", MaterialDesignA.ACCOUNT_GROUP_OUTLINE, UserRole.ADMIN, UserRole.CASHIER),
    PURCHASES("Compras", MaterialDesignT.TRUCK_OUTLINE, UserRole.ADMIN),
    CASH("Caja", MaterialDesignC.CASH_MULTIPLE, UserRole.ADMIN, UserRole.CASHIER),
    REPORTS("Reportes", MaterialDesignC.CHART_BOX_OUTLINE, UserRole.ADMIN),
    USERS("Usuarios", MaterialDesignA.ACCOUNT_COG_OUTLINE, UserRole.ADMIN),
    SETTINGS("Configuración", MaterialDesignC.COG_OUTLINE, UserRole.ADMIN);

    private final String title;
    private final Ikon icon;
    private final Set<UserRole> roles;

    Route(String title, Ikon icon, UserRole... roles) {
        this.title = title;
        this.icon = icon;
        this.roles = Set.of(roles);
    }

    public String title() {
        return title;
    }

    public Ikon icon() {
        return icon;
    }

    /** Roles que ven esta pantalla. */
    public Set<UserRole> roles() {
        return roles;
    }

    public boolean allowedFor(Collection<UserRole> userRoles) {
        return userRoles.stream().anyMatch(roles::contains);
    }

    /** Rutas visibles para esos roles, en el orden del menú. */
    public static List<Route> visibleFor(Collection<UserRole> userRoles) {
        return Arrays.stream(values())
                .filter(route -> route.allowedFor(userRoles))
                .toList();
    }
}
