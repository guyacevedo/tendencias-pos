-- Identidad: usuarios, roles y tokens de renovación. Los usuarios nunca se borran, solo se desactivan.
create table role (
    id   smallint    primary key,
    code varchar(30) not null unique,
    name varchar(60) not null
);

insert into role (id, code, name) values
    (1, 'ADMIN', 'Administrador'),
    (2, 'CASHIER', 'Cajero');

create table app_user (
    id                   bigint generated always as identity primary key,
    username             varchar(50)  not null unique check (username = lower(username)),
    full_name            varchar(120) not null,
    password_hash        varchar(255) not null,
    active               boolean      not null default true,
    failed_attempts      integer      not null default 0 check (failed_attempts >= 0),
    locked_until         timestamptz,
    must_change_password boolean      not null default false,
    created_at           timestamptz  not null default now(),
    updated_at           timestamptz  not null default now(),
    version              bigint       not null default 0
);

create table user_role (
    user_id bigint   not null references app_user (id),
    role_id smallint not null references role (id),
    primary key (user_id, role_id)
);

-- Solo se guarda el SHA-256 del token; el valor en claro lo conoce únicamente el cliente.
create table refresh_token (
    id         bigint generated always as identity primary key,
    user_id    bigint      not null references app_user (id),
    token_hash varchar(64) not null unique,
    issued_at  timestamptz not null,
    expires_at timestamptz not null,
    revoked_at timestamptz
);

create index ix_refresh_token_user on refresh_token (user_id) where revoked_at is null;
