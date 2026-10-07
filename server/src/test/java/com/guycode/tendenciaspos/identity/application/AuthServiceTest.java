package com.guycode.tendenciaspos.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guycode.tendenciaspos.identity.domain.Role;
import com.guycode.tendenciaspos.identity.domain.User;
import com.guycode.tendenciaspos.shared.web.BusinessException;
import com.guycode.tendenciaspos.shared.web.ErrorCode;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthServiceTest {
    private static final String IP = "10.0.0.1";
    private final IdentityFixture f = new IdentityFixture();

    private IssuedSession loginOk(String username, String password) {
        var result = f.auth.login(username, password, IP);
        assertThat(result).isInstanceOf(LoginResult.Success.class);
        return ((LoginResult.Success) result).session();
    }

    @Test
    void loginCorrectoEmiteAccessTokenConRolesYRefreshToken() {
        f.existing("ana", "clave-segura-1", Role.CASHIER);

        var session = loginOk("  ANA ", "clave-segura-1");

        var jwt = f.decoder.decode(session.accessToken());
        assertThat(jwt.getSubject()).isEqualTo("ana");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("CASHIER");
        assertThat(jwt.getClaimAsBoolean("pwd_change")).isFalse();
        assertThat(jwt.getExpiresAt()).isEqualTo(IdentityFixture.T0.plus(Duration.ofMinutes(15)));
        assertThat(session.refreshToken()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(session.refreshTokenExpiresAt()).isEqualTo(IdentityFixture.T0.plus(Duration.ofHours(8)));
    }

    @Test
    void claveErradaUsuarioInexistenteEInactivoRespondenIgual() {
        f.existing("ana", "clave-segura-1", Role.CASHIER);
        var inactive = f.existing("beto", "clave-segura-2", Role.CASHIER);
        inactive.deactivate(IdentityFixture.T0);
        f.users.save(inactive);

        assertThat(f.auth.login("ana", "otra-clave-x", IP)).isEqualTo(new LoginResult.InvalidCredentials());
        assertThat(f.auth.login("nadie", "clave-segura-1", IP)).isEqualTo(new LoginResult.InvalidCredentials());
        assertThat(f.auth.login("beto", "clave-segura-2", IP)).isEqualTo(new LoginResult.InvalidCredentials());
        assertThat(f.auth.login(null, null, IP)).isEqualTo(new LoginResult.InvalidCredentials());
    }

    @Test
    void cincoFallosBloqueanQuinceMinutosAunConLaClaveCorrecta() {
        f.existing("ana", "clave-segura-1", Role.CASHIER);
        for (int i = 0; i < 4; i++) {
            assertThat(f.auth.login("ana", "mala-clave-" + i, IP)).isInstanceOf(LoginResult.InvalidCredentials.class);
        }

        var fifth = f.auth.login("ana", "mala-clave-5", IP);
        var until = IdentityFixture.T0.plus(Duration.ofMinutes(15));
        assertThat(fifth).isEqualTo(new LoginResult.Locked(until));
        assertThat(f.auth.login("ana", "clave-segura-1", IP)).isEqualTo(new LoginResult.Locked(until));

        f.clock.advance(Duration.ofMinutes(15));
        loginOk("ana", "clave-segura-1");
    }

    @Test
    void refreshRotaElTokenYConservaElFinDeLaSesion() {
        f.existing("ana", "clave-segura-1", Role.CASHIER);
        var first = loginOk("ana", "clave-segura-1");
        f.clock.advance(Duration.ofMinutes(20));

        var second = refreshOk(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(second.refreshTokenExpiresAt()).isEqualTo(first.refreshTokenExpiresAt());
        assertThat(f.decoder.decode(second.accessToken()).getIssuedAt())
                .isEqualTo(IdentityFixture.T0.plus(Duration.ofMinutes(20)));
    }

    @Test
    void reusarUnRefreshTokenCierraTodasLasSesiones() {
        var ana = f.existing("ana", "clave-segura-1", Role.CASHIER);
        var first = loginOk("ana", "clave-segura-1");
        var second = refreshOk(first.refreshToken());

        assertThat(f.auth.refresh(first.refreshToken(), IP)).isInstanceOf(RefreshResult.Rejected.class);
        assertThat(f.auth.refresh(second.refreshToken(), IP)).isInstanceOf(RefreshResult.Rejected.class);
        assertThat(f.refreshTokens.activeFor(ana.id())).isZero();
    }

    @Test
    void refreshVencidoOInventadoSeRechaza() {
        f.existing("ana", "clave-segura-1", Role.CASHIER);
        var session = loginOk("ana", "clave-segura-1");

        assertThat(f.auth.refresh("no-existe", IP)).isInstanceOf(RefreshResult.Rejected.class);
        assertThat(f.auth.refresh(" ", IP)).isInstanceOf(RefreshResult.Rejected.class);
        f.clock.advance(Duration.ofHours(8));
        assertThat(f.auth.refresh(session.refreshToken(), IP)).isInstanceOf(RefreshResult.Rejected.class);
    }

    @Test
    void refreshDeUsuarioDesactivadoSeRechaza() {
        var ana = f.existing("ana", "clave-segura-1", Role.CASHIER);
        var session = loginOk("ana", "clave-segura-1");
        ana.deactivate(IdentityFixture.T0);
        f.users.save(ana);

        assertThat(f.auth.refresh(session.refreshToken(), IP)).isInstanceOf(RefreshResult.Rejected.class);
    }

    @Test
    void logoutRevocaYEsIdempotente() {
        f.existing("ana", "clave-segura-1", Role.CASHIER);
        var session = loginOk("ana", "clave-segura-1");

        f.auth.logout(session.refreshToken());
        f.auth.logout(session.refreshToken());
        f.auth.logout("desconocido");

        assertThat(f.auth.refresh(session.refreshToken(), IP)).isInstanceOf(RefreshResult.Rejected.class);
    }

    @Test
    void cambioDeClaveQuitaLaObligacionYCierraLasOtrasSesiones() {
        var admin = f.users.save(User.create(
                "admin",
                "Admin",
                IdentityFixture.ENCODER.encode("inicial-123"),
                Set.of(Role.ADMIN),
                IdentityFixture.T0));
        var first = loginOk("admin", "inicial-123");
        assertThat(f.decoder.decode(first.accessToken()).getClaimAsBoolean("pwd_change"))
                .isTrue();
        var actor = new Actor(admin.id(), "admin");

        assertThatThrownBy(() -> f.auth.changePassword(actor, "equivocada", "nueva-clave-123"))
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        assertThatThrownBy(() -> f.auth.changePassword(actor, "inicial-123", "corta"))
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.WEAK_PASSWORD);
        assertThatThrownBy(() -> f.auth.changePassword(actor, "inicial-123", "inicial-123"))
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.WEAK_PASSWORD);

        var fresh = f.auth.changePassword(actor, "inicial-123", "nueva-clave-123");

        assertThat(f.decoder.decode(fresh.accessToken()).getClaimAsBoolean("pwd_change"))
                .isFalse();
        assertThat(f.refreshTokens.activeFor(admin.id())).isEqualTo(1);
        assertThat(f.auth.refresh(first.refreshToken(), IP)).isInstanceOf(RefreshResult.Rejected.class);
        loginOk("admin", "nueva-clave-123");
    }

    private IssuedSession refreshOk(String token) {
        var result = f.auth.refresh(token, IP);
        assertThat(result).isInstanceOf(RefreshResult.Success.class);
        return ((RefreshResult.Success) result).session();
    }
}
