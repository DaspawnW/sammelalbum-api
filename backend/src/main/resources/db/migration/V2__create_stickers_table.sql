CREATE TABLE stickers (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

INSERT INTO stickers (id, name)
SELECT x, 'unknown' FROM generate_series(1, 636) AS x;
