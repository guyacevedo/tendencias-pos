package com.guycode.tendenciaspos.desktop.shell;

import static org.assertj.core.api.Assertions.assertThat;

import com.guycode.tendenciaspos.contracts.identity.UserRole;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RouteTest {
    @Test
    void elAdministradorVeTodoElMenu() {
        assertThat(Route.visibleFor(Set.of(UserRole.ADMIN))).containsExactly(Route.values());
    }

    @Test
    void elCajeroSoloVeLoQueUsa() {
        assertThat(Route.visibleFor(Set.of(UserRole.CASHIER)))
                .containsExactly(Route.HOME, Route.SALES, Route.CUSTOMERS, Route.CASH);
    }

    @Test
    void laAdministracionDeUsuariosYLaConfiguracionSonSoloDelAdministrador() {
        assertThat(Route.USERS.allowedFor(Set.of(UserRole.CASHIER))).isFalse();
        assertThat(Route.SETTINGS.allowedFor(Set.of(UserRole.CASHIER))).isFalse();
        assertThat(Route.USERS.allowedFor(Set.of(UserRole.CASHIER, UserRole.ADMIN)))
                .isTrue();
    }

    @Test
    void sinRolesNoHayMenu() {
        assertThat(Route.visibleFor(Set.of())).isEmpty();
    }
}
