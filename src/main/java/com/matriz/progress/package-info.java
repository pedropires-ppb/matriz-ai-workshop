/**
 * Progress: task counts by state and percentage Done, derived from Tasks (architecture
 * definition, Components and responsibilities). Uses Tasks, and Projects, whose interface
 * ProjectProgress it implements (module rule 2, as changed on 2026-11-09 by the I5 decisions).
 * <p>
 * This top-level package is the module's API (ADR-008): the identifiers and the interfaces that
 * other modules use or implement. Its sub-packages domain, application, infrastructure, and web
 * are internal. Empty: Progress offers nothing that other modules call directly.
 */
@ApplicationModule(displayName = "Progress", allowedDependencies = { "tasks", "projects" })
package com.matriz.progress;

import org.springframework.modulith.ApplicationModule;
