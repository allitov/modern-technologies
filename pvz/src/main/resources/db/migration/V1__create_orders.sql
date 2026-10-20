CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_code      VARCHAR(30) UNIQUE NOT NULL,
    phone           VARCHAR(20),
    parcel_article  VARCHAR(50),
    cell            VARCHAR(30),
    status          VARCHAR(20) NOT NULL DEFAULT 'ready',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    issued_at       TIMESTAMP
);
