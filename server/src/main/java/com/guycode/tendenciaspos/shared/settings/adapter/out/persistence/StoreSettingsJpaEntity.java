package com.guycode.tendenciaspos.shared.settings.adapter.out.persistence;

import com.guycode.tendenciaspos.contracts.settings.InvoiceFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "store_settings")
class StoreSettingsJpaEntity {
    /** La tabla tiene una sola fila. */
    static final short ROW_ID = 1;

    @Id
    short id = ROW_ID;

    @Column(name = "store_name", nullable = false, length = 120)
    String storeName = "";

    @Column(name = "tax_id", nullable = false, length = 20)
    String taxId = "";

    @Column(nullable = false, length = 160)
    String address = "";

    @Column(nullable = false, length = 40)
    String phone = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_format", nullable = false, length = 20)
    InvoiceFormat invoiceFormat = InvoiceFormat.RECEIPT_80MM;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt = Instant.EPOCH;

    @Version
    long version;

    protected StoreSettingsJpaEntity() {}
}
