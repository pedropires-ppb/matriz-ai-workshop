/**
 * Projects: projects, their members, and each member's role (architecture definition,
 * Components and responsibilities). Uses Identity to refer to people (module rule 2).
 * <p>
 * This top-level package is the module's API (ADR-008): the identifiers and the interfaces that
 * other modules use or implement. Its sub-packages domain, application, infrastructure, and web
 * are internal. Since I3 it holds the project, state, role, and action types, and the interface
 * through which Access control decides who may do what with a project. Since I5 also
 * ProjectProgress, which Progress implements. Since I8 also ProjectTasks, which Tasks implements, so
 * that completing a project checks its unfinished tasks without Projects depending on Tasks.
 */
@ApplicationModule(displayName = "Projects", allowedDependencies = "identity")
package com.matriz.projects;

import org.springframework.modulith.ApplicationModule;
