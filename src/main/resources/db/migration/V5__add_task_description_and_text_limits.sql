-- Matriz schema, version 5: a task's optional description, and the maximum lengths of its title
-- and description (Assumption A-033 and A-004 as amended; docs/database.md v0.7; ADR-007, open
-- point "Maximum text lengths", resolved on 2026-11-05 for tasks). Applied by Flyway. Never edit
-- this file once it has been applied; change the schema with a new migration.
--
-- The description is stored only when present: no description is NULL, never an empty text. Lengths
-- count characters, as the value objects do. The existing title check (not empty) stays. The new
-- constraints hold for the existing rows, including the demonstration data, whose titles are short.

ALTER TABLE task
    ADD COLUMN description text;

ALTER TABLE task
    ADD CONSTRAINT task_title_length_ck CHECK (char_length(title) <= 150),
    ADD CONSTRAINT task_description_present_ck CHECK (description IS NULL OR description <> ''),
    ADD CONSTRAINT task_description_length_ck CHECK (char_length(description) <= 2000);
