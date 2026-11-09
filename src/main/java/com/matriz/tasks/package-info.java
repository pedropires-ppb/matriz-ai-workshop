/**
 * Tasks: tasks, their assignments, and their comments (architecture definition, Components and
 * responsibilities). Uses Projects for project state and membership, and Identity for comment
 * authors (module rule 2; ADR-007 decision 9).
 * <p>
 * This top-level package is the module's API (ADR-008): the identifiers and the interfaces that
 * other modules use or implement. Its sub-packages domain, application, infrastructure, and web
 * are internal. In I4: the task's identifier, state, and priority, the actions of the access policy,
 * and the policy interface. Since I5: TaskCounting, the counts by state that Progress uses.
 */
@ApplicationModule(displayName = "Tasks", allowedDependencies = { "projects", "identity" })
package com.matriz.tasks;

import org.springframework.modulith.ApplicationModule;
