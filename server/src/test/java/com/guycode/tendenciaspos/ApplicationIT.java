package com.guycode.tendenciaspos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Arranca la aplicación completa contra PostgreSQL real y verifica las migraciones. */
@Tag("it")
@Testcontainers
@SpringBootTest
class ApplicationIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void aplicaMigracionesDeFlyway() {
        Integer tablas = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'audit_log'", Integer.class);
        assertThat(tablas).isEqualTo(1);
    }
}
