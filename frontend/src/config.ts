// The one place where the backend's address is configured (I13 scope decision 12). By default the API is
// served by the same origin as the User interface (ADR-004; ADR-007 decision 2). A build may set
// VITE_API_BASE_URL to another origin; that origin would then also have to allow the User interface's
// requests, which the application does not configure.

/** The base of every API path, from the configured origin. */
export function apiBase(configuredOrigin: string | undefined): string {
  return `${(configuredOrigin ?? '').trim().replace(/\/+$/, '')}/api`;
}

export const API_BASE = apiBase(import.meta.env.VITE_API_BASE_URL);

/** The address of an API path such as `/projects`. */
export function apiUrl(path: string): string {
  return `${API_BASE}${path}`;
}
