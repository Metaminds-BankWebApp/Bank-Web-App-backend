-- Email addresses must be unique across every account type.
ALTER TABLE users
    ADD CONSTRAINT users_email_key UNIQUE (email);