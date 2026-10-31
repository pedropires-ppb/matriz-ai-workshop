/**
 * The Tasks module's domain layer (ADR-008): its aggregates, entities, value objects, and
 * repository interfaces, named in the glossary's terms and mapped with Jakarta Persistence
 * annotations. It uses no other layer of any module, neither Spring nor Hibernate, and of Jakarta
 * Persistence only the mapping annotations and the enumerations they take. Other modules are used
 * only through their top-level packages (module rule 2).
 * <p>
 * Empty in I1. First classes in I4 (ADR-008).
 */
package com.matriz.tasks.domain;
