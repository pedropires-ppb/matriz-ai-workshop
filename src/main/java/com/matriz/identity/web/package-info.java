/**
 * The Identity module's web layer (ADR-008): the REST controllers and the request and response
 * types of openapi.yaml (ADR-004). Uses the application layer; never the infrastructure layer. No
 * other layer depends on it.
 * <p>
 * Since I2: signing in and out, the signed-in person, which requests need an access token, the
 * {@code 401} responses, and the resolution of {@link com.matriz.identity.SignedInPerson} for the
 * controllers of every module (ADR-005, ADR-009).
 */
package com.matriz.identity.web;
