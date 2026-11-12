import { useCallback, useEffect, useState } from 'react';
import { getCurrentPerson, signOut } from './api';
import { Link } from './components';
import HomeScreen from './HomeScreen';
import { type Session, SessionEndedError } from './http';
import { messages } from './messages';
import { type Flash, go, useLocation } from './navigation';
import ProjectScreen from './ProjectScreen';
import ProjectsScreen from './ProjectsScreen';
import { ALL_PROJECTS, HOME, type Route } from './router';
import { SessionContext } from './session';
import SignInScreen, { type SignInNotice } from './SignInScreen';
import TaskScreen from './TaskScreen';

/**
 * Signed in, with the token in memory only, or signed out, with the reason to show (ADR-005; ADR-009
 * decision 1). Reloading the page therefore starts signed out, with no message, but the address is kept,
 * so the person returns to the same screen after signing in (I13 decision 4).
 */
type State = { session: Session } | { session: null; notice: SignInNotice | null };

function screenFor(route: Route, notice: Flash | null) {
  switch (route.screen) {
    case 'home':
      return <HomeScreen notice={notice} />;
    case 'projects':
      return <ProjectsScreen route={route} notice={notice} />;
    case 'project':
      return <ProjectScreen key={route.projectId} route={route} notice={notice} />;
    case 'task':
      return <TaskScreen key={`${route.projectId}/${route.taskId}`} route={route} notice={notice} />;
    case 'unknown':
      return null;
  }
}

export default function App() {
  const [state, setState] = useState<State>({ session: null, notice: null });
  const [signingOut, setSigningOut] = useState(false);
  const { route, notice } = useLocation();
  const session = state.session;

  /** Discards the token, unless another session has replaced it in the meantime. */
  function end(ended: Session, reason: SignInNotice) {
    setState((current) => (current.session === ended ? { session: null, notice: reason } : current));
  }

  /** A 401 from any request: the session has ended, so the person signs in again, on the same address. */
  const sessionEnded = useCallback(() => {
    if (session !== null) {
      end(session, 'sessionEnded');
    }
  }, [session]);

  // An address that names no screen leads to Home (I13 decision 4).
  useEffect(() => {
    if (route.screen === 'unknown') {
      go(HOME, { replace: true });
    }
  }, [route]);

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

  // Signing out discards the token even if the request fails, and leads to Home (I13 decision 4).
  async function signOutNow(current: Session) {
    setSigningOut(true);
    try {
      await signOut(current);
    } catch {
      // The token is discarded below in every case.
    } finally {
      go(HOME, { replace: true });
      end(current, 'signedOut');
      setSigningOut(false);
    }
  }

  return (
    <>
      <header className="app-header">
        <h1>{messages.applicationName}</h1>
        {session !== null && (
          <>
            <nav aria-label={messages.mainNavigation} className="main-nav">
              <Link to={HOME} className={route.screen === 'home' ? 'current' : undefined}>
                {messages.home}
              </Link>
              <Link to={ALL_PROJECTS} className={route.screen === 'projects' ? 'current' : undefined}>
                {messages.projects}
              </Link>
            </nav>
            <p className="signed-in">
              <span>{messages.signedInAs(session.person.displayName)}</span>{' '}
              <button type="button" className="button secondary" disabled={signingOut} onClick={() => void signOutNow(session)}>
                {messages.signOut}
              </button>
            </p>
          </>
        )}
      </header>
      <main>
        {session === null ? (
          <SignInScreen
            notice={state.session === null ? state.notice : null}
            onSignedIn={(signedIn) => setState({ session: signedIn })}
          />
        ) : (
          <SessionContext.Provider value={{ session, onSessionEnded: sessionEnded }}>
            {screenFor(route, notice)}
          </SessionContext.Provider>
        )}
      </main>
    </>
  );
}
