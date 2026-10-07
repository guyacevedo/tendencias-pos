package com.guycode.tendenciaspos.shared.settings.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.guycode.tendenciaspos.contracts.settings.InvoiceFormat;
import com.guycode.tendenciaspos.shared.audit.AuditLog;
import com.guycode.tendenciaspos.shared.settings.domain.StoreSettings;
import com.guycode.tendenciaspos.shared.web.BusinessException;
import com.guycode.tendenciaspos.support.MutableClock;
import java.time.Instant;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

class StoreSettingsServiceTest {
    private static final Instant T0 = Instant.parse("2026-10-06T12:00:00Z");

    private final MutableClock clock = new MutableClock(T0);
    private final InMemoryStoreSettings repository = new InMemoryStoreSettings();
    private final StoreSettingsService service = new StoreSettingsService(repository, mock(AuditLog.class), clock);

    @Test
    void guardaLosDatosSinEspaciosYSellaLaFecha() {
        var saved = service.update(
                "admin",
                "  Tendencias Shoes  ",
                " 900123456-7 ",
                " Calle 10 # 5-20 ",
                " 3001234567 ",
                InvoiceFormat.LETTER);

        assertThat(saved.storeName()).isEqualTo("Tendencias Shoes");
        assertThat(saved.taxId()).isEqualTo("900123456-7");
        assertThat(saved.invoiceFormat()).isEqualTo(InvoiceFormat.LETTER);
        assertThat(saved.updatedAt()).isEqualTo(T0);
        assertThat(service.get()).isEqualTo(saved);
    }

    @Test
    void elNombreEsObligatorioYLosDemasCamposPuedenQuedarVacios() {
        assertThatThrownBy(() -> service.update("admin", "   ", null, null, null, InvoiceFormat.RECEIPT_80MM))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).properties().get("errors"))
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsOnlyKeys("storeName");

        var saved = service.update("admin", "Tendencias", null, null, null, InvoiceFormat.RECEIPT_80MM);
        assertThat(saved.taxId()).isEmpty();
        assertThat(saved.address()).isEmpty();
        assertThat(saved.phone()).isEmpty();
    }

    @Test
    void rechazaCamposDemasiadoLargosYFormatoAusente() {
        assertThatThrownBy(
                        () -> service.update("admin", "Tienda", "x".repeat(21), "y".repeat(161), "z".repeat(41), null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).properties().get("errors"))
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsOnlyKeys("taxId", "address", "phone", "invoiceFormat");
    }

    private static final class InMemoryStoreSettings implements StoreSettingsRepository {
        private StoreSettings current =
                new StoreSettings("Tendencias Shoes", "", "", "", InvoiceFormat.RECEIPT_80MM, T0);

        @Override
        public StoreSettings load() {
            return current;
        }

        @Override
        public StoreSettings save(StoreSettings settings) {
            current = settings;
            return current;
        }
    }
}
