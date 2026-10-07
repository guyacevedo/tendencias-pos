package com.guycode.tendenciaspos.desktop.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guycode.tendenciaspos.contracts.identity.AuthTokens;
import com.guycode.tendenciaspos.contracts.identity.SessionUser;
import com.guycode.tendenciaspos.contracts.identity.UserRole;
import com.guycode.tendenciaspos.desktop.api.ApiException;
import com.guycode.tendenciaspos.desktop.api.AuthApi;
import com.guycode.tendenciaspos.desktop.api.SessionGateway;
import com.guycode.tendenciaspos.desktop.api.SettingsApi;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SessionManagerTest {
    private static final Instant T0 = Instant.parse("2026-10-06T12:00:00Z");
    private static final Instant SESSION_END = T0.plus(Duration.ofHours(8));

    private final TestClock clock = new TestClock(T0);
    private final FakeGateway gateway = new FakeGateway();
    private final SessionManager session = new SessionManager(gateway, clock);
    private final List<String> ended = new ArrayList<>();

    SessionManagerTest() {
        session.onSessionEnded(ended::add);
        session.start(tokens("access-1", T0.plus(Duration.ofMinutes(15)), "refresh-1"));
    }

    @Test
    void usaElAccessTokenMientrasEstaVigente() {
        assertThat(session.headerFor(SettingsApi.SETTINGS_PATH)).isEqualTo("Bearer access-1");
        assertThat(gateway.refreshed).isEmpty();
        assertThat(session.hasRole(UserRole.ADMIN)).isTrue();
        assertThat(session.hasRole(UserRole.CASHIER)).isFalse();
    }

    @Test
    void noAgregaCredencialNiRenuevaEnLasRutasPublicas() {
        clock.advance(Duration.ofMinutes(15));

        assertThat(session.headerFor(AuthApi.LOGIN_PATH)).isNull();
        assertThat(session.headerFor(AuthApi.REFRESH_PATH)).isNull();
        assertThat(gateway.refreshed).isEmpty();
    }

    @Test
    void renuevaElAccessTokenCuandoLeQuedaMenosDelMargen() {
        clock.advance(Duration.ofMinutes(14).plusSeconds(1));

        assertThat(session.headerFor(SettingsApi.SETTINGS_PATH)).isEqualTo("Bearer access-2");
        assertThat(gateway.refreshed).containsExactly("refresh-1");

        // La renovación anterior ya dejó un token fresco: no se vuelve a usar el refresh.
        assertThat(session.headerFor(SettingsApi.SETTINGS_PATH)).isEqualTo("Bearer access-2");
        assertThat(gateway.refreshed).containsExactly("refresh-1");
    }

    @Test
    void siElRefreshYaNoSirveTerminaLaSesion() {
        gateway.failRefreshWith = new ApiException("INVALID_REFRESH_TOKEN", "La sesión ya no es válida.", 401);
        clock.advance(Duration.ofMinutes(15));

        assertThatThrownBy(() -> session.headerFor(SettingsApi.SETTINGS_PATH))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(SessionManager.SESSION_ENDED_CODE);
        assertThat(ended).containsExactly(SessionManager.SESSION_ENDED);
        assertThat(session.isActive()).isFalse();
    }

    @Test
    void sinConexionConservaLaSesionYPropagaElError() {
        gateway.failRefreshWith = new ApiException(ApiException.NETWORK_ERROR, "Sin conexión.", 0);
        clock.advance(Duration.ofMinutes(15));

        assertThatThrownBy(() -> session.headerFor(SettingsApi.SETTINGS_PATH))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(ApiException.NETWORK_ERROR);
        assertThat(session.isActive()).isTrue();
        assertThat(ended).isEmpty();
    }

    @Test
    void un401EnUnaRutaConSesionLaTerminaYEnLoginNo() {
        session.rejected(AuthApi.LOGIN_PATH);
        assertThat(session.isActive()).isTrue();
        assertThat(ended).isEmpty();

        session.rejected(SettingsApi.SETTINGS_PATH);
        assertThat(session.isActive()).isFalse();
        assertThat(ended).containsExactly(SessionManager.SESSION_ENDED);

        session.rejected(SettingsApi.SETTINGS_PATH);
        assertThat(ended).hasSize(1);
    }

    @Test
    void cerrarSesionRevocaElRefreshYNoFallaSiNoHayRed() {
        session.logout();

        assertThat(gateway.revoked).containsExactly("refresh-1");
        assertThat(session.isActive()).isFalse();
        assertThat(session.user()).isNull();
        assertThat(session.roles()).isEmpty();

        gateway.failLogoutWith = new ApiException(ApiException.NETWORK_ERROR, "Sin conexión.", 0);
        session.start(tokens("access-9", T0, "refresh-9"));
        session.logout();
        assertThat(session.isActive()).isFalse();
    }

    private static AuthTokens tokens(String access, Instant accessExpiry, String refresh) {
        var user = new SessionUser(1, "admin", "Administradora", Set.of(UserRole.ADMIN), false);
        return new AuthTokens(access, accessExpiry, refresh, SESSION_END, user);
    }

    private static final class FakeGateway implements SessionGateway {
        final List<String> refreshed = new ArrayList<>();
        final List<String> revoked = new ArrayList<>();
        ApiException failRefreshWith;
        ApiException failLogoutWith;
        int issued = 1;

        @Override
        public AuthTokens refresh(String refreshToken) {
            refreshed.add(refreshToken);
            if (failRefreshWith != null) {
                throw failRefreshWith;
            }
            issued++;
            return tokens(
                    "access-" + issued,
                    Instant.parse("2026-10-06T12:00:00Z").plus(Duration.ofHours(1)),
                    "refresh-" + issued);
        }

        @Override
        public void logout(String refreshToken) {
            revoked.add(refreshToken);
            if (failLogoutWith != null) {
                throw failLogoutWith;
            }
        }
    }

    private static final class TestClock extends Clock {
        private Instant now;

        TestClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
