-- Matriz schema, version 2: the technical table of access tokens ended by signing out
-- (ADR-005, open point "How sign-out is enforced", resolved on 2026-11-02; docs/database.md
-- v0.5). Applied by Flyway. Never edit this file once it has been applied; change the schema
-- with a new migration.
--
-- A row holds only the random identifier of an ended token (its jti claim) and the time at
-- which the token expires; it holds no token and refers to no person. A token whose identifier
-- is here is refused until it expires. Each sign-out deletes the rows whose expiry has passed.

CREATE TABLE ended_access_token (
    token_id   uuid        NOT NULL,
    expires_at timestamptz NOT NULL,
    CONSTRAINT ended_access_token_pk PRIMARY KEY (token_id)
);
