package com.guycode.tendenciaspos.desktop.settings;

import static org.assertj.core.api.Assertions.assertThat;

import com.guycode.tendenciaspos.contracts.settings.InvoiceFormat;
import com.guycode.tendenciaspos.contracts.settings.StoreSettingsResponse;
import com.guycode.tendenciaspos.contracts.settings.UpdateStoreSettingsRequest;
import com.guycode.tendenciaspos.desktop.api.ApiException;
import com.guycode.tendenciaspos.desktop.api.SettingsGateway;
import com.guycode.tendenciaspos.desktop.core.UiExecutor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettingsPresenterTest {
    private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");
    private static final StoreSettingsResponse SETTINGS = new StoreSettingsResponse(
            "Tendencias Shoes", "900123456-7", "Calle 10 # 5-20", "3001234567", InvoiceFormat.RECEIPT_80MM, NOW);

    private final RecordingView view = new RecordingView();
    private final UiExecutor direct = new UiExecutor(Runnable::run, Runnable::run);
    private final FakeGateway gateway = new FakeGateway();
    private final SettingsPresenter presenter = new SettingsPresenter(view, gateway, direct);

    @Test
    void cargaLaConfiguracion() {
        presenter.load();

        assertThat(view.calls).containsExactly("loading:true", "loading:false", "settings:Tendencias Shoes");
    }

    @Test
    void guardaYAvisa() {
        presenter.save("Tendencias Shoes SAS", "900123456-7", "Calle 10", "300", InvoiceFormat.LETTER);

        assertThat(gateway.saved)
                .containsExactly(new UpdateStoreSettingsRequest(
                        "Tendencias Shoes SAS", "900123456-7", "Calle 10", "300", InvoiceFormat.LETTER));
        assertThat(view.calls)
                .containsExactly("errors:{}", "loading:true", "loading:false", "settings:Tendencias Shoes", "saved");
    }

    @Test
    void muestraLosErroresPorCampoQueDevuelveElServidor() {
        gateway.failWith = new ApiException(
                "VALIDATION_FAILED",
                "Hay datos inválidos.",
                400,
                Map.of("errors", Map.of("storeName", "El nombre de la tienda es obligatorio.")),
                null);

        presenter.save("  ", "", "", "", InvoiceFormat.LETTER);

        assertThat(view.calls)
                .endsWith("errors:{storeName=El nombre de la tienda es obligatorio.}")
                .doesNotContain("saved");
    }

    @Test
    void sinConexionMuestraUnMensajeGeneral() {
        gateway.failWith = new ApiException(ApiException.NETWORK_ERROR, "detalle técnico", 0);

        presenter.load();

        assertThat(view.calls).endsWith("error:No se pudo contactar al servidor.");
    }

    private static final class FakeGateway implements SettingsGateway {
        final List<UpdateStoreSettingsRequest> saved = new ArrayList<>();
        ApiException failWith;

        @Override
        public StoreSettingsResponse get() {
            if (failWith != null) {
                throw failWith;
            }
            return SETTINGS;
        }

        @Override
        public StoreSettingsResponse update(UpdateStoreSettingsRequest request) {
            saved.add(request);
            if (failWith != null) {
                throw failWith;
            }
            return SETTINGS;
        }
    }

    private static final class RecordingView implements SettingsView {
        final List<String> calls = new ArrayList<>();

        @Override
        public void showLoading(boolean loading) {
            calls.add("loading:" + loading);
        }

        @Override
        public void showSettings(StoreSettingsResponse settings) {
            calls.add("settings:" + settings.storeName());
        }

        @Override
        public void showFieldErrors(Map<String, String> errors) {
            calls.add("errors:" + errors);
        }

        @Override
        public void showError(String message) {
            calls.add("error:" + message);
        }

        @Override
        public void showSaved() {
            calls.add("saved");
        }
    }
}
