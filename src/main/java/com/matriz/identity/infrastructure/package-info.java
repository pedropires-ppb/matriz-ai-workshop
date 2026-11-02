/**
 * The Identity module's infrastructure layer (ADR-008): the implementations of the domain's
 * repository interfaces, with Spring Data JPA, and the implementations of interfaces that other
 * modules define. Never uses the web layer. Only Flyway migrations create or change the schema.
 * <p>
 * Since I2: the person repository, password checking with BCrypt, and the signed access tokens:
 * their signing key, issuing, verification, and the table of ended tokens (ADR-005).
 */
package com.matriz.identity.infrastructure;
