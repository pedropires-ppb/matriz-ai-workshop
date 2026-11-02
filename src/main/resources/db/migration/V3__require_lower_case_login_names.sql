-- Matriz schema, version 3: login names are case-insensitive, so they are stored in lower case
-- (ADR-007, open point "Case of login names", resolved on 2026-11-02; docs/database.md v0.5).
-- Applied by Flyway. Never edit this file once it has been applied; change the schema with a
-- new migration.
--
-- Signing in lower-cases the login name it receives before looking it up. The existing unique
-- constraint then also makes login names unique regardless of letter case.

ALTER TABLE person
    ADD CONSTRAINT person_login_name_lower_case_ck CHECK (login_name = lower(login_name));
