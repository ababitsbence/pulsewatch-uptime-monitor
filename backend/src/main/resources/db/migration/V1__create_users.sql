CREATE TABLE users (
       id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
       email         VARCHAR(255) NOT NULL UNIQUE,
       password_hash VARCHAR(100) NOT NULL,
       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);