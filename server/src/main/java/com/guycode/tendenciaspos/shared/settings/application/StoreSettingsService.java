package com.guycode.tendenciaspos.shared.settings.application;

import com.guycode.tendenciaspos.contracts.settings.InvoiceFormat;
import com.guycode.tendenciaspos.shared.audit.AuditLog;
import com.guycode.tendenciaspos.shared.settings.domain.StoreSettings;
import com.guycode.tendenciaspos.shared.web.BusinessException;
import java.time.Clock;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lectura y actualización de los datos de la tienda (la escritura es solo para ADMIN). */
@Service
public class StoreSettingsService {
    private final StoreSettingsRepository repository;
    private final AuditLog audit;
    private final Clock clock;

    StoreSettingsService(StoreSettingsRepository repository, AuditLog audit, Clock clock) {
        this.repository = repository;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public StoreSettings get() {
        return repository.load();
    }

    @Transactional
    public StoreSettings update(
            String actor, String storeName, String taxId, String address, String phone, InvoiceFormat invoiceFormat) {
        var errors = StoreSettings.validate(storeName, taxId, address, phone, invoiceFormat);
        if (!errors.isEmpty()) {
            throw BusinessException.validation(errors);
        }
        var saved =
                repository.save(new StoreSettings(storeName, taxId, address, phone, invoiceFormat, clock.instant()));
        audit.record(
                actor,
                "SETTINGS_UPDATED",
                "STORE_SETTINGS",
                1,
                Map.of(
                        "storeName",
                        saved.storeName(),
                        "invoiceFormat",
                        saved.invoiceFormat().name()));
        return saved;
    }
}
