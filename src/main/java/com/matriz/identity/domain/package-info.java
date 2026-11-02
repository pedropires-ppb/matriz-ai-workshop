/**
 * The Identity module's domain layer (ADR-008): its aggregates, entities, value objects, and
 * repository interfaces, named in the glossary's terms and mapped with Jakarta Persistence
 * annotations. It uses no other layer of any module, neither Spring nor Hibernate, and of Jakarta
 * Persistence only the mapping annotations and the enumerations they take. Other modules are used
 * only through their top-level packages (module rule 2).
 * <p>
 * Since I2: the Person aggregate and its value objects, and the interfaces for checking passwords,
 * issuing access tokens, and recording ended tokens (ADR-005).
 */
package com.matriz.identity.domain;
