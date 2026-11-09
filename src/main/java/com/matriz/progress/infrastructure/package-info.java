/**
 * The Progress module's infrastructure layer (ADR-008): the implementations of the domain's
 * repository interfaces, with Spring Data JPA, and the implementations of interfaces that other
 * modules define. Never uses the web layer. Only Flyway migrations create or change the schema.
 * <p>
 * Empty: Progress has no table and no query of its own (ADR-008, layers of Progress).
 */
package com.matriz.progress.infrastructure;
