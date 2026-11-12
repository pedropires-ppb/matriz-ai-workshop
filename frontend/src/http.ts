// What every call to the Matriz API (openapi.yaml) shares. The access token is kept only in memory, by
// the caller, and is sent only in the Authorization header (ADR-005). A 401 to a request made with the
// token throws `SessionEndedError`, which sends the person to sign in (ADR-009); the other refusals are
// returned, not thrown, so that a screen can show them.
import { apiUrl } from './config';

export interface PersonSummary {
  personId: string;
  displayName: string;
}

/** A signed-in person's access token, when it expires, and who it names. */
export interface Session {
  accessToken: string;
  /** When the token expires, in milliseconds since the epoch, from `expiresIn`. */
  expiresAt: number;
  person: PersonSummary;
}

/** The answer 401 to a request made with the access token: the session is no longer valid. */
export class SessionEndedError extends Error {
  constructor() {
    super('The session is no longer valid.');
    this.name = 'SessionEndedError';
  }
}

/** A member of the content that the API refused, by its JSON Pointer (ValidationProblem). */
export interface FieldProblem {
  pointer: string;
  detail: string;
}

/** What the API did with a request. */
export type Result<T> =
  | { outcome: 'ok'; value: T }
  | { outcome: 'invalid'; problems: FieldProblem[] }
  | { outcome: 'notFound' }
  | { outcome: 'forbidden' }
  | { outcome: 'completed' }
  | { outcome: 'failed' };

export type Refused = Exclude<Result<unknown>, { outcome: 'ok' }>;

export const JSON_TYPE = 'application/json';
export const MERGE_PATCH_TYPE = 'application/merge-patch+json';

/** A request made with the access token. Every 401 to it means that the session has ended. */
export async function withToken(session: Session, path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set('Authorization', `Bearer ${session.accessToken}`);
  const response = await fetch(apiUrl(path), { ...init, headers });
  if (response.status === 401) {
    throw new SessionEndedError();
  }
  return response;
}

/** The parts of a request that sends `body` as JSON, or as a JSON merge patch. */
export function sending(method: string, body: unknown, type: string = JSON_TYPE): RequestInit {
  return { method, headers: { 'Content-Type': type }, body: JSON.stringify(body) };
}

/** Reads a response: its content, or `null` for an operation that answers without content. */
export async function answer<T>(response: Response, withoutContent = false): Promise<Result<T>> {
  if (response.ok) {
    return { outcome: 'ok', value: (withoutContent ? null : await response.json()) as T };
  }
  switch (response.status) {
    case 404:
      return { outcome: 'notFound' };
    case 403:
      return { outcome: 'forbidden' };
    case 409:
      return { outcome: 'completed' };
    case 422: {
      const problem = (await response.json().catch(() => ({}))) as { errors?: FieldProblem[] };
      return { outcome: 'invalid', problems: problem.errors ?? [] };
    }
    default:
      return { outcome: 'failed' };
  }
}

/** Sends a request with the token and reads its answer. */
export async function call<T>(
  session: Session,
  path: string,
  init: RequestInit = {},
  withoutContent = false,
): Promise<Result<T>> {
  return answer<T>(await withToken(session, path, init), withoutContent);
}
