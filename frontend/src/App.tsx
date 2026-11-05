import { useCallback, useEffect, useState } from 'react';
import { getCurrentPerson, type Session, SessionEndedError, signOut } from './api';
import { messages } from './messages';
import ProjectScreen from './ProjectScreen';
import ProjectsScreen from './ProjectsScreen';
import SignInScreen, { type Notice } from './SignInScreen';

/**
 * Signed in, with the token in memory only, and the project shown (none: My projects), or signed
 * out, with the reason to show (ADR-005; ADR-009 decision 1). Reloading the page therefore starts
 * signed out, with no message.
 */
type State = { session: Session; projectId: string | null } | { session: null; notice: Notice | null };

export default function App() {
  const [state, setState] = useState<State>({ session: null, notice: null });
  const [signingOut, setSigningOut] = useState(false);
  const session = state.session;

  /** Discards the token, unless another session has replaced it in the meantime. */
  function end(ended: Session, notice: Notice) {
    setState((current) => (current.session === ended ? { session: null, notice } : current));
  }

  /** A 401 from any request: the session has ended, so the person signs in again. */
  const sessionEnded = useCallback(() => {
    if (session !== null) {
      end(session, 'sessionEnded');
    }
  }, [session]);

  // The token is discarded when it expires, without waiting for a request.
  useEffect(() => {
    if (session === null) {
      return;
    }
    const timer = window.setTimeout(() => end(session, 'sessionEnded'), Math.max(0, session.expiresAt - Date.now()));
    return () => window.clearTimeout(timer);
  }, [session]);

  // When the page becomes visible again, the token is checked with GET /api/me.
  useEffect(() => {
    if (session === null) {
      return;
    }
    function check() {
      if (session !== null && document.visibilityState === 'visible') {
        getCurrentPerson(session).catch((error: unknown) => {
          if (error instanceof SessionEndedError) {
            end(session, 'sessionEnded');
          }
        });
      }
    }
    document.addEventListener('visibilitychange', check);
    return () => document.removeEventListener('visibilitychange', check);
  }, [session]);

  // Signing out discards the token even if the request fails.
  async function signOutNow(current: Session) {
    setSigningOut(true);
    try {
      await signOut(current);
    } catch {
      // The token is discarded below in every case.
    } finally {
      end(current, 'signedOut');
      setSigningOut(false);
    }
  }

  return (
    <>
      <header>
        <h1>{messages.applicationName}</h1>
        {session !== null && (
          <p>
            <span>{messages.signedInAs(session.person.displayName)}</span>{' '}
            <button type="button" disabled={signingOut} onClick={() => void signOutNow(session)}>
              {messages.signOut}
            </button>
          </p>
        )}
      </header>
      <main>
        {session === null ? (
          <SignInScreen
            notice={state.session === null ? state.notice : null}
            onSignedIn={(signedIn) => setState({ session: signedIn, projectId: null })}
          />
        ) : (
          state.session !== null && state.projectId !== null ? (
            <ProjectScreen
              key={state.projectId}
              session={session}
              projectId={state.projectId}
              onBack={() => setState({ session, projectId: null })}
              onSessionEnded={sessionEnded}
            />
          ) : (
            <ProjectsScreen
              session={session}
              onOpen={(projectId) => setState({ session, projectId })}
              onSessionEnded={sessionEnded}
            />
          )
        )}
      </main>
    </>
  );
}
