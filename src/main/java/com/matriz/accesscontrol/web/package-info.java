/**
 * The Access control module's web layer (ADR-008): the REST controllers and the request and
 * response types of openapi.yaml (ADR-004). Uses the application layer; never the infrastructure
 * layer. No other layer depends on it.
 * <p>
 * Empty in I1. First classes in I3, if the access policy needs this layer (ADR-008).
 */
package com.matriz.accesscontrol.web;
