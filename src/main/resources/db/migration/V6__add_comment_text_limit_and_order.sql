-- Matriz schema, version 6: the maximum length of a comment's text, and the order of comments
-- (Assumption A-036; docs/database.md; ADR-007; ADR-008). Applied by Flyway. Never edit this file
-- once it has been applied; change the schema with a new migration.
--
-- The length counts characters, as the value object does. The existing check (not empty) stays. The
-- constraint holds for the existing rows, including the demonstration data, whose texts are short.
-- creation_order is given by the database and only breaks ties between comments added in the same
-- millisecond: it is never exposed by the API. It is filled for the rows that already exist.

ALTER TABLE comment
    ADD COLUMN creation_order bigint GENERATED ALWAYS AS IDENTITY;

ALTER TABLE comment
    ADD CONSTRAINT comment_text_length_ck CHECK (char_length(text) <= 2000);

CREATE INDEX comment_task_order_ix ON comment (task_id, created_at, creation_order);
