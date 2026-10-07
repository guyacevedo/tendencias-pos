package com.guycode.tendenciaspos.shared.settings.adapter.out.persistence;

import com.guycode.tendenciaspos.shared.settings.application.StoreSettingsRepository;
import com.guycode.tendenciaspos.shared.settings.domain.StoreSettings;
import org.springframework.stereotype.Repository;

@Repository
class StoreSettingsPersistenceAdapter implements StoreSettingsRepository {
    private final StoreSettingsJpaRepository rows;

    StoreSettingsPersistenceAdapter(StoreSettingsJpaRepository rows) {
        this.rows = rows;
    }

    @Override
    public StoreSettings load() {
        return toDomain(row());
    }

    @Override
    public StoreSettings save(StoreSettings settings) {
        var entity = row();
        entity.storeName = settings.storeName();
        entity.taxId = settings.taxId();
        entity.address = settings.address();
        entity.phone = settings.phone();
        entity.invoiceFormat = settings.invoiceFormat();
        entity.updatedAt = settings.updatedAt();
        return toDomain(rows.save(entity));
    }

    /** La fila la crea la migración; si faltara, se recrea en blanco en lugar de fallar. */
    private StoreSettingsJpaEntity row() {
        return rows.findById(StoreSettingsJpaEntity.ROW_ID).orElseGet(StoreSettingsJpaEntity::new);
    }

    private static StoreSettings toDomain(StoreSettingsJpaEntity e) {
        return new StoreSettings(e.storeName, e.taxId, e.address, e.phone, e.invoiceFormat, e.updatedAt);
    }
}
