/**
 * Progress: task counts by state and percentage Done, derived from Tasks (architecture
 * definition, Components and responsibilities). Uses Tasks (module rule 2).
 * <p>
 * This top-level package is the module's API (ADR-008): the identifiers and the interfaces that
 * other modules use or implement. Its sub-packages domain, application, infrastructure, and web
 * are internal. Empty in I1.
 */
@ApplicationModule(displayName = "Progress", allowedDependencies = "tasks")
package com.matriz.progress;

import org.springframework.modulith.ApplicationModule;
