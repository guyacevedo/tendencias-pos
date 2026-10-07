package com.guycode.tendenciaspos.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserTest {
    private static final Instant T0 = Instant.parse("2026-10-06T12:00:00Z");
    private final LockoutPolicy policy = LockoutPolicy.DEFAULT;

    private User user() {
        return User.create("ana", "Ana", "hash", Set.of(Role.CASHIER), T0);
    }

    @Test
    void usuarioNuevoActivoYDebeCambiarClave() {
        var u = user();
        assertThat(u.active()).isTrue();
        assertThat(u.mustChangePassword()).isTrue();
        assertThat(u.isLocked(T0)).isFalse();
    }

    @Test
    void quintoFalloBloqueaQuinceMinutos() {
        var u = user();
        for (int i = 0; i < 4; i++) {
            assertThat(u.registerFailedLogin(T0, policy)).isFalse();
        }
        assertThat(u.registerFailedLogin(T0, policy)).isTrue();
        assertThat(u.isLocked(T0.plus(Duration.ofMinutes(14)))).isTrue();
        assertThat(u.isLocked(T0.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void trasVencerElBloqueoHayCincoIntentosNuevos() {
        var u = user();
        for (int i = 0; i < 5; i++) {
            u.registerFailedLogin(T0, policy);
        }
        var later = T0.plus(Duration.ofMinutes(16));
        assertThat(u.registerFailedLogin(later, policy)).isFalse();
        assertThat(u.lockedUntil()).isNull();
        assertThat(u.failedAttempts()).isEqualTo(1);
    }

    @Test
    void ingresoCorrectoReiniciaElContador() {
        var u = user();
        u.registerFailedLogin(T0, policy);
        u.registerSuccessfulLogin(T0);
        assertThat(u.failedAttempts()).isZero();
    }

    @Test
    void restablecerClaveDesbloqueaYObligaACambiarla() {
        var u = user();
        u.changePassword("h2", T0);
        for (int i = 0; i < 5; i++) {
            u.registerFailedLogin(T0, policy);
        }
        u.resetPassword("h3", T0);
        assertThat(u.isLocked(T0)).isFalse();
        assertThat(u.mustChangePassword()).isTrue();
    }

    @Test
    void exigeAlMenosUnRol() {
        assertThatIllegalArgumentException().isThrownBy(() -> User.create("ana", "Ana", "h", Set.of(), T0));
    }

    @Test
    void politicaDeClave() {
        assertThat(PasswordPolicy.isAcceptable("123456789")).isFalse();
        assertThat(PasswordPolicy.isAcceptable("1234567890")).isTrue();
        assertThat(PasswordPolicy.isAcceptable("          ")).isFalse();
        assertThat(PasswordPolicy.isAcceptable("x".repeat(129))).isFalse();
        assertThat(PasswordPolicy.isAcceptable(null)).isFalse();
    }

    @Test
    void nombreDeUsuarioSeNormaliza() {
        assertThat(Username.normalize("  Ana.Perez ")).isEqualTo("ana.perez");
        assertThat(Username.isValid("ana.perez")).isTrue();
        assertThat(Username.isValid("ab")).isFalse();
        assertThat(Username.isValid("josé")).isFalse();
    }
}
