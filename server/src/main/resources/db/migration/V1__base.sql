-- Registro de auditoría: quién hizo qué y cuándo. Solo se inserta, nunca se modifica.
create table audit_log (
    id          bigint generated always as identity primary key,
    occurred_at timestamptz  not null default now(),
    actor       varchar(100),
    action      varchar(60)  not null,
    entity_type varchar(60),
    entity_id   varchar(60),
    details     jsonb
);

create index ix_audit_log_occurred_at on audit_log (occurred_at desc);
