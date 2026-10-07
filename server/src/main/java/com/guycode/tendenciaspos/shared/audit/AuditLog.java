package com.guycode.tendenciaspos.shared.audit;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registro de auditoría en {@code audit_log}. Se escribe dentro de la transacción del caso de uso, así que
 * si la operación se revierte, su auditoría también.
 */
@Component
public class AuditLog {
    private static final int ACTOR_MAX = 100;

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final Clock clock;

    AuditLog(JdbcClient jdbc, JsonMapper json, Clock clock) {
        this.jdbc = jdbc;
        this.json = json;
        this.clock = clock;
    }

    /**
     * @param actor usuario que hizo la acción (o el nombre intentado en un login fallido)
     * @param action acción en mayúsculas, p. ej. {@code LOGIN_SUCCESS}
     * @param entityType tipo de entidad afectada, o {@code null}
     * @param entityId id de la entidad afectada, o {@code null}
     * @param details datos adicionales sin información sensible (nunca claves ni tokens)
     */
    public void record(String actor, String action, String entityType, Object entityId, Map<String, ?> details) {
        jdbc.sql("""
                        insert into audit_log (occurred_at, actor, action, entity_type, entity_id, details)
                        values (:occurredAt, :actor, :action, :entityType, :entityId, cast(:details as jsonb))
                        """)
                .param("occurredAt", clock.instant().atOffset(ZoneOffset.UTC))
                .param("actor", truncate(actor))
                .param("action", action)
                .param("entityType", entityType)
                .param("entityId", entityId == null ? null : entityId.toString())
                .param("details", details == null || details.isEmpty() ? null : json.writeValueAsString(details))
                .update();
    }

    private static String truncate(String actor) {
        if (actor == null) {
            return null;
        }
        return actor.length() <= ACTOR_MAX ? actor : actor.substring(0, ACTOR_MAX);
    }
}
