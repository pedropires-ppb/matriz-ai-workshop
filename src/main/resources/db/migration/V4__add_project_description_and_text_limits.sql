-- Matriz schema, version 4: a project's optional description, and the maximum length of its
-- name (Assumption A-031; docs/database.md v0.6; ADR-007, open point "Maximum text lengths",
-- resolved on 2026-11-02 for projects). Applied by Flyway. Never edit this file once it has been
-- applied; change the schema with a new migration.
--
-- The description is stored only when present: no description is NULL, never an empty text. Lengths
-- count characters, as the value objects do. The new constraints hold for the existing rows,
-- including the demonstration data, whose names are short.

ALTER TABLE project
    ADD COLUMN description text;

ALTER TABLE project
    ADD CONSTRAINT project_name_length_ck CHECK (char_length(name) <= 100),
    ADD CONSTRAINT project_description_present_ck CHECK (description IS NULL OR description <> ''),
    ADD CONSTRAINT project_description_length_ck CHECK (char_length(description) <= 1000);
