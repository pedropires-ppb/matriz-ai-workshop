/**
 * Identity: the people known to Matriz, signing in and out, and the signed-in person
 * (architecture definition, Components and responsibilities). Depends on no other module.
 * <p>
 * This top-level package is the module's API (ADR-008): the identifiers and the interfaces that
 * other modules use or implement. Its sub-packages domain, application, infrastructure, and web
 * are internal. Since I2 it holds {@link com.matriz.identity.PersonId} and
 * {@link com.matriz.identity.SignedInPerson}, through which other modules learn who is acting
 * (ADR-009 decision 3).
 */
@ApplicationModule(displayName = "Identity", allowedDependencies = {})
package com.matriz.identity;

import org.springframework.modulith.ApplicationModule;
