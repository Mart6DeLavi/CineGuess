ALTER TABLE users
    ALTER COLUMN username TYPE varchar(255),
    ADD COLUMN email varchar(255),
    ADD COLUMN password_hash varchar(255);

UPDATE users
SET email = username
WHERE email IS NULL;

ALTER TABLE users
    ALTER COLUMN email SET NOT NULL,
    ADD CONSTRAINT uq_users_email UNIQUE (email);
