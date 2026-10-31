/**
 * The Progress module's domain layer (ADR-008). Progress has no aggregate: this layer holds the
 * classes of its read model, if any. It uses no other layer of any module, neither Spring nor
 * Hibernate, and of Jakarta Persistence only the mapping annotations and the enumerations they
 * take. Other modules are used only through their top-level packages (module rule 2).
 * <p>
 * Empty in I1. First classes in I5, if the Progress read model needs this layer (ADR-008).
 */
package com.matriz.progress.domain;
