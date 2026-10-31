-- Matriz demonstration data (ADR-007 decision 10, amended on 2026-10-30). Fictitious people and
-- examples from docs/user-stories.md; no real personal data.
--
-- Flyway applies this migration only when MATRIZ_DEMO_DATA=true, and only once per database: a
-- demonstration row that someone changes or deletes later is not restored. Version 1.1 places it
-- right after the schema migration it was written for (V1). Never edit this file once it has
-- been applied; add a new demonstration migration instead.
--
-- The password hashes are Flyway placeholders. At each start-up with demonstration data, Matriz
-- hashes the passwords in MATRIZ_DEMO_PASSWORD_ANA, _RUI, _EVA, and _DANIEL; no password and no
-- hash is stored in this file (ADR-005 decision 6).

-- Identity --------------------------------------------------------------------------------

INSERT INTO person (person_id, login_name, display_name, password_hash) VALUES
    ('10000000-0000-4000-8000-000000000001', 'ana', 'Ana Silva', '${demo_password_hash_ana}'),
    ('10000000-0000-4000-8000-000000000002', 'rui', 'Rui Martins', '${demo_password_hash_rui}'),
    ('10000000-0000-4000-8000-000000000003', 'eva', 'Eva Santos', '${demo_password_hash_eva}'),
    ('10000000-0000-4000-8000-000000000004', 'daniel', 'Daniel Rocha', '${demo_password_hash_daniel}');

-- Projects --------------------------------------------------------------------------------

INSERT INTO project (project_id, name, state) VALUES
    ('20000000-0000-4000-8000-000000000001', 'Website Redesign', 'ACTIVE'),
    ('20000000-0000-4000-8000-000000000002', 'Mobile App', 'ACTIVE'),
    ('20000000-0000-4000-8000-000000000003', 'Website Audit', 'ACTIVE'),
    ('20000000-0000-4000-8000-000000000004', 'Project Gamma', 'COMPLETED');

-- Daniel Rocha is a member of no project.
INSERT INTO membership (project_id, person_id, role) VALUES
    ('20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'PROJECT_OWNER'),
    ('20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000002', 'PROJECT_PARTICIPANT'),
    ('20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000003', 'PROJECT_PARTICIPANT'),
    ('20000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000002', 'PROJECT_OWNER'),
    ('20000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 'PROJECT_PARTICIPANT'),
    ('20000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000003', 'PROJECT_OWNER'),
    ('20000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000001', 'PROJECT_OWNER'),
    ('20000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000002', 'PROJECT_PARTICIPANT');

-- Tasks -----------------------------------------------------------------------------------

-- One statement per task, so that the database generates creation_order in this order.
INSERT INTO task (task_id, project_id, title, state, priority) VALUES
    ('30000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 'T1', 'TO_DO', 'HIGH');
INSERT INTO task (task_id, project_id, title, state, priority) VALUES
    ('30000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000001', 'T2', 'IN_PROGRESS', 'HIGH');
INSERT INTO task (task_id, project_id, title, state, priority) VALUES
    ('30000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000001', 'T3', 'DONE', 'LOW');
INSERT INTO task (task_id, project_id, title, state, priority) VALUES
    ('30000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000001', 'T4', 'TO_DO', 'MEDIUM');
INSERT INTO task (task_id, project_id, title, state, priority) VALUES
    ('30000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000002', 'Prepare proposal', 'TO_DO', 'MEDIUM');
INSERT INTO task (task_id, project_id, title, state, priority) VALUES
    ('30000000-0000-4000-8000-000000000006', '20000000-0000-4000-8000-000000000002', 'Review proposal', 'IN_PROGRESS', 'HIGH');
INSERT INTO task (task_id, project_id, title, state, priority) VALUES
    ('30000000-0000-4000-8000-000000000007', '20000000-0000-4000-8000-000000000004', 'T1', 'DONE', 'MEDIUM');
INSERT INTO task (task_id, project_id, title, state, priority) VALUES
    ('30000000-0000-4000-8000-000000000008', '20000000-0000-4000-8000-000000000004', 'T2', 'DONE', 'MEDIUM');
INSERT INTO task (task_id, project_id, title, state, priority) VALUES
    ('30000000-0000-4000-8000-000000000009', '20000000-0000-4000-8000-000000000004', 'T3', 'DONE', 'MEDIUM');

INSERT INTO assignment (task_id, person_id, project_id) VALUES
    ('30000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001'),
    ('30000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000001'),
    ('30000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002');

INSERT INTO comment (comment_id, task_id, author_person_id, text, created_at) VALUES
    ('40000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000006',
     '10000000-0000-4000-8000-000000000001', 'First', '2026-10-29T09:30:00Z'),
    ('40000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000006',
     '10000000-0000-4000-8000-000000000002', 'Second', '2026-10-29T10:05:00Z');
