package com.guycode.tendenciaspos.shared.web;

import com.guycode.tendenciaspos.contracts.ApiVersion;
import com.guycode.tendenciaspos.shared.config.TposProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Versión del servidor y mínima del cliente; pública para que el escritorio la consulte al iniciar. */
@RestController
@RequestMapping("/api/version")
class VersionController {
    private final String serverVersion;
    private final String minClientVersion;

    VersionController(ObjectProvider<BuildProperties> build, TposProperties props) {
        var info = build.getIfAvailable();
        this.serverVersion = info != null ? info.getVersion() : "dev";
        this.minClientVersion = props.minClientVersion();
    }

    @GetMapping
    ApiVersion version() {
        return new ApiVersion(serverVersion, minClientVersion);
    }
}
