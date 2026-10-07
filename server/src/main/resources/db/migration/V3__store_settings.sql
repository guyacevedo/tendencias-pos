-- Configuración de la tienda: una sola fila (id = 1) con los datos que encabezan facturas y reportes.
create table store_settings (
    id             smallint     primary key default 1 check (id = 1),
    store_name     varchar(120) not null,
    tax_id         varchar(20)  not null default '',
    address        varchar(160) not null default '',
    phone          varchar(40)  not null default '',
    invoice_format varchar(20)  not null check (invoice_format in ('LETTER', 'RECEIPT_80MM')),
    updated_at     timestamptz  not null default now(),
    version        bigint       not null default 0
);

insert into store_settings (id, store_name, invoice_format) values (1, 'Tendencias Shoes', 'RECEIPT_80MM');
