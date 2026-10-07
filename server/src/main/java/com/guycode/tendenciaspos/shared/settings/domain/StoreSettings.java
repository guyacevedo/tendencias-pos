package com.guycode.tendenciaspos.shared.settings.domain;

import com.guycode.tendenciaspos.contracts.settings.InvoiceFormat;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Datos de la tienda. Solo el nombre es obligatorio: el NIT, la dirección y el teléfono se completan
 * cuando la tienda los tiene y pueden quedar vacíos.
 */
public record StoreSettings(
        String storeName, String taxId, String address, String phone, InvoiceFormat invoiceFormat, Instant updatedAt) {
    public static final int NAME_MAX = 120;
    public static final int TAX_ID_MAX = 20;
    public static final int ADDRESS_MAX = 160;
    public static final int PHONE_MAX = 40;

    public StoreSettings {
        Objects.requireNonNull(invoiceFormat, "invoiceFormat");
        storeName = clean(storeName);
        taxId = clean(taxId);
        address = clean(address);
        phone = clean(phone);
    }

    /** Mensajes por campo inválido; vacío si los datos sirven. */
    public static Map<String, String> validate(
            String storeName, String taxId, String address, String phone, InvoiceFormat invoiceFormat) {
        var errors = new LinkedHashMap<String, String>();
        if (clean(storeName).isEmpty()) {
            errors.put("storeName", "El nombre de la tienda es obligatorio.");
        } else if (clean(storeName).length() > NAME_MAX) {
            errors.put("storeName", "Máximo " + NAME_MAX + " caracteres.");
        }
        tooLong(errors, "taxId", taxId, TAX_ID_MAX);
        tooLong(errors, "address", address, ADDRESS_MAX);
        tooLong(errors, "phone", phone, PHONE_MAX);
        if (invoiceFormat == null) {
            errors.put("invoiceFormat", "Elija el formato de factura.");
        }
        return errors;
    }

    private static void tooLong(Map<String, String> errors, String field, String value, int max) {
        if (clean(value).length() > max) {
            errors.put(field, "Máximo " + max + " caracteres.");
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
