package com.guycode.tendenciaspos.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj inyectable para que las reglas con tiempo (bloqueos, vencimientos) se prueben sin esperar. */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
