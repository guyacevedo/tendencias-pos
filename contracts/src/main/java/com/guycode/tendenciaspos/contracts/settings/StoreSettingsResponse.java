package com.guycode.tendenciaspos.contracts.settings;

import java.time.Instant;

/** Datos de la tienda que encabezan facturas y reportes ({@code GET /api/settings}). */
public record StoreSettingsResponse(
        String storeName, String taxId, String address, String phone, InvoiceFormat invoiceFormat, Instant updatedAt) {}
