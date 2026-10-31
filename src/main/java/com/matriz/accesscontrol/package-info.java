/**
 * Access control: whether a signed-in person may list, view, or change a project or task
 * (architecture definition, Components and responsibilities). Uses Identity, Projects, and Tasks
 * (module rule 2).
 * <p>
 * This top-level package is the module's API (ADR-008): the identifiers and the interfaces that
 * other modules use or implement. Its sub-packages domain, application, infrastructure, and web
 * are internal. Empty in I1.
 */
@ApplicationModule(displayName = "Access control", allowedDependencies = { "identity", "projects", "tasks" })
package com.matriz.accesscontrol;

import org.springframework.modulith.ApplicationModule;
