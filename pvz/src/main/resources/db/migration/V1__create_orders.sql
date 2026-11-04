create table if not exists orders (
    id              bigint generated always as identity primary key,
    order_code      varchar(30) unique not null,
    phone           varchar(20),
    parcel_article  varchar(50),
    cell            varchar(30),
    status          varchar(20) not null default 'ready',
    created_at      timestamptz not null default now(),
    issued_at       timestamptz
);

comment on table orders is 'Таблица заказов.';
comment on column orders.id is 'Уникальный идентификатор заказа.';
comment on column orders.order_code is 'Код заказа вида PVZ-000001.';
comment on column orders.phone is 'Номер телефона. Пустая строка, если заказ принят по артикулу.';
comment on column orders.parcel_article is 'Артикул посылки. Пустая строка, если заказ принят по номеру телефона.';
comment on column orders.cell is 'Код ячейки хранения.';
comment on column orders.status is 'Уникальный идентификатор заказа.';
comment on column orders.created_at is 'Дата поступления заказа.';
comment on column orders.issued_at is 'Дата выдачи заказа.';
