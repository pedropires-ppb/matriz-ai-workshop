/**
 * The Progress module's web layer (ADR-008): the REST controllers and the request and response
 * types of openapi.yaml (ADR-004). Uses the application layer; never the infrastructure layer. No
 * other layer depends on it.
 * <p>
 * Empty: the progress is returned by Projects' operations (openapi.yaml, Project and
 * ProjectSummary).
 */
package com.matriz.progress.web;
