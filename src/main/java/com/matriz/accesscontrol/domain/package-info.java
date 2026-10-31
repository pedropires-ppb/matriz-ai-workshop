/**
 * The Access control module's domain layer (ADR-008). Access control has no aggregate: this layer
 * holds the classes of the access policy, if any. It uses no other layer of any module, neither
 * Spring nor Hibernate, and of Jakarta Persistence only the mapping annotations and the
 * enumerations they take. Other modules are used only through their top-level packages (module
 * rule 2).
 * <p>
 * Empty in I1. First classes in I3, if the access policy needs this layer (ADR-008).
 */
package com.matriz.accesscontrol.domain;
