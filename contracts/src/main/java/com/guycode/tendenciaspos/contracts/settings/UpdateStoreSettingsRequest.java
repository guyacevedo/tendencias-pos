package com.guycode.tendenciaspos.contracts.settings;

/** {@code PUT /api/settings} (solo ADMIN). */
public record UpdateStoreSettingsRequest(
        String storeName, String taxId, String address, String phone, InvoiceFormat invoiceFormat) {}
