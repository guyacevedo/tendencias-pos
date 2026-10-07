package com.guycode.tendenciaspos.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guycode.tendenciaspos.identity.domain.Role;
import com.guycode.tendenciaspos.shared.web.BusinessException;
import com.guycode.tendenciaspos.shared.web.ErrorCode;
import java.util.Set;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class UserAdminServiceTest {
    private final IdentityFixture f = new IdentityFixture();
    private final Actor admin =
            new Actor(f.existing("admin", "clave-admin-1", Role.ADMIN).id(), "admin");

    private static void assertCode(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(code);
    }

    @Test
    void creaCajeroQueDebeCambiarLaClave() {
        var user = f.admin.create(admin, " Cajero1 ", " Luis Pérez ", "temporal-123", Set.of(Role.CASHIER));

        assertThat(user.username()).isEqualTo("cajero1");
        assertThat(user.fullName()).isEqualTo("Luis Pérez");
        assertThat(user.mustChangePassword()).isTrue();
        assertThat(f.auth.login("cajero1", "temporal-123", "ip")).isInstanceOf(LoginResult.Success.class);
    }

    @Test
    void validaDatosClaveYDuplicados() {
        assertThatThrownBy(() -> f.admin.create(admin, "a", "", "temporal-123", Set.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).properties().get("errors"))
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsOnlyKeys("username", "fullName", "roles");
        assertCode(() -> f.admin.create(admin, "luis", "Luis", "corta", Set.of(Role.CASHIER)), ErrorCode.WEAK_PASSWORD);
        assertCode(
                () -> f.admin.create(admin, "ADMIN", "Otro", "temporal-123", Set.of(Role.CASHIER)),
                ErrorCode.USERNAME_TAKEN);
    }

    @Test
    void noPuedeQuedarSinAdministradorActivo() {
        assertCode(() -> f.admin.update(admin, admin.userId(), "Admin", Set.of(Role.CASHIER)), ErrorCode.LAST_ADMIN);

        var other = f.admin.create(admin, "jefe", "Jefe", "temporal-123", Set.of(Role.ADMIN));
        f.admin.deactivate(admin, other.id());
        assertCode(() -> f.admin.update(admin, admin.userId(), "Admin", Set.of(Role.CASHIER)), ErrorCode.LAST_ADMIN);

        f.admin.activate(admin, other.id());
        var updated = f.admin.update(admin, admin.userId(), "Admin", Set.of(Role.CASHIER));
        assertThat(updated.roles()).containsExactly(Role.CASHIER);
    }

    @Test
    void noSePuedeDesactivarASiMismo() {
        assertCode(() -> f.admin.deactivate(admin, admin.userId()), ErrorCode.CONFLICT);
    }

    @Test
    void desactivarCierraSesionesEImpideEntrar() {
        var cashier = f.admin.create(admin, "luis", "Luis", "temporal-123", Set.of(Role.CASHIER));
        f.auth.login("luis", "temporal-123", "ip");

        var result = f.admin.deactivate(admin, cashier.id());

        assertThat(result.active()).isFalse();
        assertThat(f.refreshTokens.activeFor(cashier.id())).isZero();
        assertThat(f.auth.login("luis", "temporal-123", "ip")).isInstanceOf(LoginResult.InvalidCredentials.class);
    }

    @Test
    void restablecerClaveDesbloqueaYObligaACambiarla() {
        var cashier = f.existing("luis", "clave-luis-1", Role.CASHIER);
        for (int i = 0; i < 5; i++) {
            f.auth.login("luis", "mala-" + i, "ip");
        }

        f.admin.resetPassword(admin, cashier.id(), "temporal-456");

        var session = ((LoginResult.Success) f.auth.login("luis", "temporal-456", "ip")).session();
        assertThat(session.user().mustChangePassword()).isTrue();
        assertCode(() -> f.admin.resetPassword(admin, cashier.id(), "123"), ErrorCode.WEAK_PASSWORD);
    }

    @Test
    void usuarioInexistente() {
        assertCode(() -> f.admin.get(999), ErrorCode.NOT_FOUND);
        assertThat(f.admin.list()).extracting("username").containsExactly("admin");
    }
}
