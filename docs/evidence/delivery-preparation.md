# Delivery Preparation Evidence

This record contains observed results for the six delivery-preparation exercises. Record commands and outcomes as run; never include secret values, tokens, or real personal data. A plan or expected result is not execution evidence.

## Exercise 1 — Technical analysis and plan

- **Date:** 2026-11-14
- **Branch:** `delivery-preparation`
- **Base commit:** `bf8eb12f827cd23da567b88cf33e128dab7d48c7` (`origin/main` after fetch)
- **Plan:** [Matriz Delivery Preparation Plan](../delivery-plan.md)
- **Working tree:** tracked working tree clean before documentation edits; no unrelated changes were present. Existing ignored generated output was not staged.
- **Host architecture:** `arm64`.
- **Commands run:**

  ```sh
  git status --short --branch
  git branch --list delivery-preparation main
  git log -1 --oneline --decorate
  git fetch origin main
  git switch -c delivery-preparation origin/main
  git rev-parse HEAD
  uname -m
  ```

- **Results:** `main` and fetched `origin/main` were at `bf8eb12`; `delivery-preparation` was created at that commit and tracks `origin/main`; `uname -m` returned `arm64`.
- **Safety:** No local `.env` was read or used. No Docker daemon, containers, volumes, or ports were inspected or changed. No files were staged before this evidence was written.
- **Not run:** Maven build/tests, Docker image build, Compose startup/configuration, OpenAPI validation, and GitHub Actions. No current-branch CI result is asserted. The older I12 verification record is evidence for its recorded earlier branch/commit only.
- **Limitations:** Docker/Compose versions and availability of ports 18089/15432 were not checked. Image digests and multi-platform support remain to be verified during an authorized implementation exercise.

## Exercises 2–6

No execution evidence yet. Add a dated section for each exercise only after it is requested and run; include exact non-secret commands, outcomes, limitations, and checks not executed.

## Exercise 2 — Docker containerization

- **Date:** 2026-11-14
- **Branch/base:** `delivery-preparation`, based on Exercise 1 commit `25d1a8c`.
- **Files:** `Dockerfile` and `.dockerignore`; the Dockerfile uses digest-pinned Temurin 25 Noble builder/runtime images, packages the integrated frontend and backend with the Maven Wrapper, and copies only the executable JAR into a non-root JRE runtime. The runtime's writable home and Java temporary directory are `/tmp`.
- **Verification:** Per the user's instruction, no Docker build, image inspection, container startup, or runtime check was run for this update. No successful image build or runtime behavior is claimed. Previous unsuccessful build diagnostics are omitted here at the user's request.
- **Runtime checks not performed:** PostgreSQL readiness and migrations, application HTTP response, static assets, sign-in, authorized project/task operations, missing-configuration failure, image metadata/history, and credential absence from a built image.
- **Resources:** No image was pushed or published. The isolated fictitious `.env.delivery` remains ignored. No files under `Flutter-repos` were changed.
