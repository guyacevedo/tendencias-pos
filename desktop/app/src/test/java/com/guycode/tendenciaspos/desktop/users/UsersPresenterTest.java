package com.guycode.tendenciaspos.desktop.users;

import static org.assertj.core.api.Assertions.assertThat;

import com.guycode.tendenciaspos.contracts.identity.UserResponse;
import com.guycode.tendenciaspos.contracts.identity.UserRole;
import com.guycode.tendenciaspos.desktop.api.ApiException;
import com.guycode.tendenciaspos.desktop.api.UserGateway;
import com.guycode.tendenciaspos.desktop.core.UiExecutor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UsersPresenterTest {
    private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");
    private static final long ADMIN_ID = 1;

    private final RecordingView view = new RecordingView();
    private final UiExecutor direct = new UiExecutor(Runnable::run, Runnable::run);
    private final FakeGateway gateway = new FakeGateway();
    private final UsersPresenter presenter = new UsersPresenter(view, gateway, direct, ADMIN_ID);

    private static UserResponse user(long id, String username, boolean active, UserRole role) {
        return new UserResponse(id, username, "Nombre " + username, Set.of(role), active, null, false, NOW, NOW);
    }

    @Test
    void cargaLaLista() {
        presenter.load();

        assertThat(view.calls).containsExactly("loading:true", "loading:false", "users:admin,cajero1");
    }

    @Test
    void validaElFormularioAntesDeCrear() {
        presenter.startNew();
        presenter.save("", "", "corta".toCharArray(), Set.of());

        assertThat(gateway.created).isEmpty();
        assertThat(view.calls).endsWith("errors:[username, fullName, password, roles]");
    }

    @Test
    void creaElUsuarioYRecargaLaLista() {
        presenter.startNew();
        presenter.save("  Cajero2 ", "  Luis Pérez ", "temporal-123".toCharArray(), Set.of(UserRole.CASHIER));

        assertThat(gateway.created).containsExactly("Cajero2/Luis Pérez/temporal-123/[CASHIER]");
        assertThat(view.calls).contains("message:Usuario creado.", "users:admin,cajero1,cajero2");
    }

    @Test
    void alEditarSoloEnviaNombreYRoles() {
        presenter.load();
        presenter.select(2);
        presenter.save("cajero1", "Luis Alberto", new char[0], Set.of(UserRole.CASHIER, UserRole.ADMIN));

        assertThat(gateway.updated).containsExactly("2/Luis Alberto/[ADMIN, CASHIER]");
        assertThat(view.calls).contains("message:Usuario actualizado.");
    }

    @Test
    void restableceLaClaveConLongitudMinima() {
        presenter.load();
        presenter.select(2);

        presenter.resetPassword("corta".toCharArray());
        assertThat(gateway.reset).isEmpty();
        assertThat(view.calls).endsWith("errors:[password]");

        presenter.resetPassword("temporal-456".toCharArray());
        assertThat(gateway.reset).containsExactly("2/temporal-456");
        assertThat(view.calls).endsWith("message:" + UsersPresenter.PASSWORD_RESET);
    }

    @Test
    void desactivaOtroUsuarioPeroNuncaElPropio() {
        presenter.load();
        presenter.select(2);
        presenter.toggleActive();
        assertThat(gateway.deactivated).containsExactly(2L);

        presenter.select(ADMIN_ID);
        presenter.toggleActive();
        assertThat(gateway.deactivated).containsExactly(2L);
        assertThat(view.calls).endsWith("error:" + UsersPresenter.SELF_DEACTIVATION);
    }

    @Test
    void muestraElConflictoQueDevuelveElServidor() {
        gateway.failWith = new ApiException("LAST_ADMIN", "Debe quedar al menos un administrador activo.", 409);
        presenter.load();
        presenter.select(ADMIN_ID);
        presenter.save("admin", "Administradora", new char[0], Set.of(UserRole.CASHIER));

        assertThat(view.calls).endsWith("error:Debe quedar al menos un administrador activo.");
    }

    private static final class FakeGateway implements UserGateway {
        final List<UserResponse> users = new ArrayList<>(
                List.of(user(ADMIN_ID, "admin", true, UserRole.ADMIN), user(2, "cajero1", true, UserRole.CASHIER)));
        final List<String> created = new ArrayList<>();
        final List<String> updated = new ArrayList<>();
        final List<String> reset = new ArrayList<>();
        final List<Long> deactivated = new ArrayList<>();
        ApiException failWith;

        @Override
        public List<UserResponse> list() {
            return List.copyOf(users);
        }

        @Override
        public UserResponse create(String username, String fullName, String password, Set<UserRole> roles) {
            fail();
            created.add(username + "/" + fullName + "/" + password + "/" + sorted(roles));
            var saved = user(users.size() + 1L, username.toLowerCase(java.util.Locale.ROOT), true, UserRole.CASHIER);
            users.add(saved);
            return saved;
        }

        @Override
        public UserResponse update(long id, String fullName, Set<UserRole> roles) {
            fail();
            updated.add(id + "/" + fullName + "/" + sorted(roles));
            return users.stream().filter(u -> u.id() == id).findFirst().orElseThrow();
        }

        @Override
        public void resetPassword(long id, String newPassword) {
            fail();
            reset.add(id + "/" + newPassword);
        }

        @Override
        public UserResponse deactivate(long id) {
            fail();
            deactivated.add(id);
            return users.stream().filter(u -> u.id() == id).findFirst().orElseThrow();
        }

        @Override
        public UserResponse activate(long id) {
            fail();
            return users.stream().filter(u -> u.id() == id).findFirst().orElseThrow();
        }

        private void fail() {
            if (failWith != null) {
                throw failWith;
            }
        }

        private static List<String> sorted(Set<UserRole> roles) {
            return roles.stream().map(Enum::name).sorted().toList();
        }
    }

    private static final class RecordingView implements UsersView {
        final List<String> calls = new ArrayList<>();

        @Override
        public void showLoading(boolean loading) {
            calls.add("loading:" + loading);
        }

        @Override
        public void showUsers(List<UserResponse> users) {
            calls.add("users:"
                    + users.stream()
                            .map(UserResponse::username)
                            .toList()
                            .toString()
                            .replaceAll("[\\[\\] ]", ""));
        }

        @Override
        public void showForm(UserResponse user) {
            calls.add("form:" + (user == null ? "nuevo" : user.username()));
        }

        @Override
        public void showFieldErrors(Map<String, String> errors) {
            if (!errors.isEmpty()) {
                calls.add("errors:" + new LinkedHashMap<>(errors).keySet());
            }
        }

        @Override
        public void showError(String message) {
            calls.add("error:" + message);
        }

        @Override
        public void showMessage(String message) {
            calls.add("message:" + message);
        }
    }
}
