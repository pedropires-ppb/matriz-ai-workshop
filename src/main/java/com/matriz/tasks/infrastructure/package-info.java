/**
 * The Tasks module's infrastructure layer (ADR-008): the implementations of the domain's
 * repository interfaces, with Spring Data JPA, and the implementations of interfaces that other
 * modules define. Never uses the web layer. Only Flyway migrations create or change the schema.
 * <p>
 * Empty in I1. First classes in I4 (ADR-008).
 */
package com.matriz.tasks.infrastructure;
