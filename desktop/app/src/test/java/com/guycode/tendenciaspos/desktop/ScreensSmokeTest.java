package com.guycode.tendenciaspos.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import com.guycode.tendenciaspos.contracts.identity.UserResponse;
import com.guycode.tendenciaspos.contracts.identity.UserRole;
import com.guycode.tendenciaspos.contracts.settings.InvoiceFormat;
import com.guycode.tendenciaspos.contracts.settings.StoreSettingsResponse;
import com.guycode.tendenciaspos.desktop.login.ChangePasswordPanel;
import com.guycode.tendenciaspos.desktop.login.LoginPanel;
import com.guycode.tendenciaspos.desktop.settings.SettingsPanel;
import com.guycode.tendenciaspos.desktop.users.UsersPanel;
import com.guycode.tendenciaspos.ui.Theme;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Las pantallas se arman con el tema puesto y aceptan los datos de la API. Cubre lo que no se ve en una
 * prueba de presentador: estilos de FlatLaf y restricciones de MigLayout fallan al construir, no al
 * compilar.
 */
class ScreensSmokeTest {
    private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");

    @Test
    void cadaPantallaSeArmaConElTemaPuesto() {
        Theme.install();

        var login = new LoginPanel();
        login.showBusy(true);
        login.showError("Usuario o clave incorrectos.");
        login.clearError();
        login.clearPassword();
        login.showBusy(false);

        var change = new ChangePasswordPanel();
        change.setMandatory(true);
        change.showBusy(true);
        change.showError("La clave actual no es correcta.");
        change.clearError();
        change.clearFields();
        change.showBusy(false);

        var settings = new SettingsPanel();
        settings.showLoading(true);
        settings.showSettings(new StoreSettingsResponse(
                "Tendencias Shoes", "900123456-7", "Calle 10 # 5-20", "3001234567", InvoiceFormat.LETTER, NOW));
        settings.showLoading(false);
        settings.showFieldErrors(Map.of("storeName", "El nombre de la tienda es obligatorio."));
        settings.showError("No se pudo contactar al servidor.");
        assertThat(settings.storeNameValue()).isEqualTo("Tendencias Shoes");
        assertThat(settings.invoiceFormatValue()).isEqualTo(InvoiceFormat.LETTER);
        settings.setEditable(false);

        var users = new UsersPanel();
        users.showLoading(true);
        users.showUsers(List.of(admin(), cashier()));
        users.showLoading(false);
        users.showForm(cashier());
        assertThat(users.usernameValue()).isEqualTo("cajero1");
        assertThat(users.rolesValue()).containsExactly(UserRole.CASHIER);
        users.showForm(null);
        assertThat(users.usernameValue()).isEmpty();
        users.showFieldErrors(Map.of("username", "El usuario es obligatorio."));
        users.showError("Debe quedar al menos un administrador activo.");
    }

    private static UserResponse admin() {
        return new UserResponse(1, "admin", "Administradora", Set.of(UserRole.ADMIN), true, null, false, NOW, NOW);
    }

    private static UserResponse cashier() {
        return new UserResponse(2, "cajero1", "Luis Pérez", Set.of(UserRole.CASHIER), false, NOW, true, NOW, NOW);
    }
}
