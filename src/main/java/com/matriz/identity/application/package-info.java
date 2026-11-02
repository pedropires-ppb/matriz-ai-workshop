/**
 * The Identity module's application layer (ADR-008): its use cases, each one transaction
 * (ADR-006), checking the rules that span aggregates. Uses the domain layer and the top-level
 * packages of the modules it may use; never the infrastructure or web layers.
 * <p>
 * Since I2: signing in, signing out, and showing the signed-in person (FR-013 to FR-015).
 */
package com.matriz.identity.application;
