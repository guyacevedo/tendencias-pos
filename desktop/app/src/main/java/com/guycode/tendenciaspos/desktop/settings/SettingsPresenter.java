package com.guycode.tendenciaspos.desktop.settings;

import com.guycode.tendenciaspos.contracts.settings.InvoiceFormat;
import com.guycode.tendenciaspos.contracts.settings.UpdateStoreSettingsRequest;
import com.guycode.tendenciaspos.desktop.api.ApiException;
import com.guycode.tendenciaspos.desktop.api.SettingsGateway;
import com.guycode.tendenciaspos.desktop.core.ApiErrors;
import com.guycode.tendenciaspos.desktop.core.ErrorCodes;
import com.guycode.tendenciaspos.desktop.core.UiExecutor;
import java.util.Map;

/** Lee y guarda los datos de la tienda; la validación de campos la responde el servidor. */
public final class SettingsPresenter {
    private final SettingsView view;
    private final SettingsGateway gateway;
    private final UiExecutor executor;

    public SettingsPresenter(SettingsView view, SettingsGateway gateway, UiExecutor executor) {
        this.view = view;
        this.gateway = gateway;
        this.executor = executor;
    }

    public void load() {
        view.showLoading(true);
        executor.submit(
                gateway::get,
                settings -> {
                    view.showLoading(false);
                    view.showSettings(settings);
                },
                this::onError);
    }

    public void save(String storeName, String taxId, String address, String phone, InvoiceFormat invoiceFormat) {
        view.showFieldErrors(Map.of());
        view.showLoading(true);
        var request = new UpdateStoreSettingsRequest(storeName, taxId, address, phone, invoiceFormat);
        executor.submit(
                () -> gateway.update(request),
                saved -> {
                    view.showLoading(false);
                    view.showSettings(saved);
                    view.showSaved();
                },
                this::onError);
    }

    private void onError(Exception error) {
        view.showLoading(false);
        if (error instanceof ApiException api && ErrorCodes.VALIDATION_FAILED.equals(api.code())) {
            var errors = api.fieldErrors();
            if (!errors.isEmpty()) {
                view.showFieldErrors(errors);
                return;
            }
        }
        view.showError(ApiErrors.messageFor(error));
    }
}
