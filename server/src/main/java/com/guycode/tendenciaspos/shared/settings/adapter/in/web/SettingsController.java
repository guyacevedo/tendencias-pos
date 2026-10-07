package com.guycode.tendenciaspos.shared.settings.adapter.in.web;

import com.guycode.tendenciaspos.contracts.settings.StoreSettingsResponse;
import com.guycode.tendenciaspos.contracts.settings.UpdateStoreSettingsRequest;
import com.guycode.tendenciaspos.shared.settings.application.StoreSettingsService;
import com.guycode.tendenciaspos.shared.settings.domain.StoreSettings;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Datos de la tienda: los lee cualquier usuario autenticado; solo ADMIN los cambia. */
@RestController
@RequestMapping("/api/settings")
class SettingsController {
    private final StoreSettingsService service;

    SettingsController(StoreSettingsService service) {
        this.service = service;
    }

    @GetMapping
    StoreSettingsResponse get() {
        return toResponse(service.get());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    StoreSettingsResponse update(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateStoreSettingsRequest body) {
        return toResponse(service.update(
                jwt.getSubject(), body.storeName(), body.taxId(), body.address(), body.phone(), body.invoiceFormat()));
    }

    private static StoreSettingsResponse toResponse(StoreSettings s) {
        return new StoreSettingsResponse(
                s.storeName(), s.taxId(), s.address(), s.phone(), s.invoiceFormat(), s.updatedAt());
    }
}
