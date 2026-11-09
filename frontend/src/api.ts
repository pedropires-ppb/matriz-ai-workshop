// The session operations of the Matriz API (openapi.yaml): signing in and out, and the signed-in person.
import { apiUrl } from './config';
import { type PersonSummary, type Session, withToken } from './http';

export type SignInResult =
  | { outcome: 'signedIn'; session: Session }
  | { outcome: 'incorrect' }
  | { outcome: 'required' }
  | { outcome: 'unreachable' };

interface SignInResponse {
  accessToken: string;
  expiresIn: number;
  person: PersonSummary;
}

/** `POST /api/session` (signIn). */
export async function signIn(loginName: string, password: string): Promise<SignInResult> {
  try {
    const response = await fetch(apiUrl('/session'), {
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
  const response = await withToken(session, '/me');
  if (!response.ok) {
    throw new Error(`GET /api/me answered ${response.status}.`);
  }
  return (await response.json()) as PersonSummary;
}

/** `DELETE /api/session` (signOut): ends the token on the server. */
export async function signOut(session: Session): Promise<void> {
  await withToken(session, '/session', { method: 'DELETE' });
}
