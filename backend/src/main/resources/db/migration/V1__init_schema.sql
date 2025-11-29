CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    firstname VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL,
    mail VARCHAR(255) NOT NULL UNIQUE,
    contact VARCHAR(255)
);

CREATE TABLE credentials (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_credentials_user FOREIGN KEY (user_id) REFERENCES users (id)
);
