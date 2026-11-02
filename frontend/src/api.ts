// Calls to the Matriz API (openapi.yaml). The access token is kept only in memory, by the caller,
// and is sent only in the Authorization header (ADR-005).

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

export type SignInResult =
  | { outcome: 'signedIn'; session: Session }
  | { outcome: 'incorrect' }
  | { outcome: 'required' }
  | { outcome: 'unreachable' };

/** The answer 401 to a request made with the access token: the session is no longer valid. */
export class SessionEndedError extends Error {
  constructor() {
    super('The session is no longer valid.');
    this.name = 'SessionEndedError';
  }
}

interface SignInResponse {
  accessToken: string;
  expiresIn: number;
  person: PersonSummary;
}

/** `POST /api/session` (signIn). */
export async function signIn(loginName: string, password: string): Promise<SignInResult> {
  try {
    const response = await fetch('/api/session', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ loginName, password }),
    });
    if (response.status === 200) {
      const body = (await response.json()) as SignInResponse;
      return {
        outcome: 'signedIn',
        session: {
          accessToken: body.accessToken,
          expiresAt: Date.now() + body.expiresIn * 1000,
          person: body.person,
        },
      };
    }
    if (response.status === 401) {
      return { outcome: 'incorrect' };
    }
    if (response.status === 422) {
      return { outcome: 'required' };
    }
    return { outcome: 'unreachable' };
  } catch {
    return { outcome: 'unreachable' };
  }
}

/** `GET /api/me` (getCurrentPerson). Throws `SessionEndedError` if the token is refused. */
export async function getCurrentPerson(session: Session): Promise<PersonSummary> {
  const response = await withToken(session, '/api/me');
  if (!response.ok) {
    throw new Error(`GET /api/me answered ${response.status}.`);
  }
  return (await response.json()) as PersonSummary;
}

/** `DELETE /api/session` (signOut): ends the token on the server. */
export async function signOut(session: Session): Promise<void> {
  await withToken(session, '/api/session', { method: 'DELETE' });
}

/** A request made with the access token. Every 401 to it means that the session has ended. */
async function withToken(session: Session, path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set('Authorization', `Bearer ${session.accessToken}`);
  const response = await fetch(path, { ...init, headers });
  if (response.status === 401) {
    throw new SessionEndedError();
  }
  return response;
}
