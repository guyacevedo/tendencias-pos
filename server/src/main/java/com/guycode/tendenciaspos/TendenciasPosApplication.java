package com.guycode.tendenciaspos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TendenciasPosApplication {
    public static void main(String[] args) {
        SpringApplication.run(TendenciasPosApplication.class, args);
    }
}
