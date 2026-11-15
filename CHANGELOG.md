# Changelog

Changes are derived from repository commits, not inferred client acceptance. Candidate identity and build-version mapping are authoritative in the [release-candidate notes](docs/releases/0.1.0-mvp-rc.2.md). No tag or release is created by this file.

## 0.1.0-mvp-rc.2 — Proposed, unreleased

### Added

- Multi-stage, digest-pinned Java 25 Docker build for the single application containing the React assets; non-root JRE runtime and secret/build-output exclusions (`f97e62f`, `75e4187`).
- Opt-in Compose application profile with service-DNS database connection, initial PostgreSQL health dependency and persistent volume reuse (`84088f6`).
- Docker image build gated on mandatory verification, with no image publication (`abadacc`).
- Distinct compilation, frontend unit, backend unit, integration/contract/architecture and browser checks; actual suite/test summaries and sanitized short-lived report artifacts (`4378296`, `9a0c31a`).
- Approved delivery plan and evidence register, distinguishing agent execution from user-reported validation (`25d1a8c`, `f9a335a`).

### Fixed

- Database-only Compose no longer requires the inactive app profile's signing key during interpolation; application startup still rejects a missing key (`abadacc`).
- Runtime home and Java temporary paths are writable by the non-root container user (`75e4187`).
- README container operation, CI descriptions and clean-copy branch reference; deployment diagram and candidate preparation documentation (Exercise 5).

### Compatibility and limitations

- No application API, authentication, business-rule or database-schema change in delivery preparation. Existing host-run Java and database-only Compose use remain supported.
- Demo activation must still be selected before the first application startup against a fresh database; existing database credentials are not rotated by env-file edits.
- Image building is packaging, not runtime verification. An earlier local builder certificate error and subsequent user-confirmed Docker/Compose/CI results are recorded in the [evidence](docs/evidence/delivery-preparation.md); no new runtime/CI result is asserted here.
- I12 remains Awaiting review; client validation and production hosting remain pending. The controlled CI failure/recovery demonstration has no execution evidence.

## Earlier candidate baseline

The existing tag `v0.1.0-mvp-rc.1` points to `08e531f`, before the current delivery preparation. This changelog does not reconstruct a complete earlier release history. The intervening `bf8eb12` commit added testing/review records and strengthened existing task-domain/API/browser assertions without changing production behavior. The proposed-MVP features and their provisional scope are documented in the [README](README.md#proposed-mvp-at-a-glance), not newly introduced by RC.2.
