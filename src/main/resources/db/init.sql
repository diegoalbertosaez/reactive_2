DROP TABLE IF EXISTS health;

CREATE TABLE health
(
    id INT PRIMARY KEY,
    up BOOLEAN
);

INSERT INTO health
    (id, up)
VALUES (1, true);

CREATE TABLE IF NOT EXISTS authorized_users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_authorized_users_username ON authorized_users(username);
CREATE INDEX IF NOT EXISTS idx_authorized_users_email ON authorized_users(email);