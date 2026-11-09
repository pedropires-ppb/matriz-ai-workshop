// What every screen shares: the session, a request that ends the session on a 401, a list or record
// that is loaded with its states, and a submission that runs once at a time (I13 scope decisions 9–10).
import { createContext, type DependencyList, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { Gate } from './gate';
import { type Refused, type Result, type Session, SessionEndedError } from './http';

interface SessionContextValue {
  session: Session;
  onSessionEnded: () => void;
}

export const SessionContext = createContext<SessionContextValue | null>(null);

export function useSession(): SessionContextValue {
  const value = useContext(SessionContext);
  if (value === null) {
    throw new Error('useSession is used outside a signed-in screen.');
  }
  return value;
}

/** Runs a request with the session; a 401 sends the person to sign in, and any other error is `failed`. */
export function useApi() {
  const { session, onSessionEnded } = useSession();
  const run = useCallback(
    async <T,>(request: (session: Session) => Promise<Result<T>>): Promise<Result<T>> => {
      try {
        return await request(session);
      } catch (error: unknown) {
        if (error instanceof SessionEndedError) {
          onSessionEnded();
        }
        return { outcome: 'failed' };
      }
    },
    [session, onSessionEnded],
  );
  return { session, run };
}

/**
 * What a screen has loaded. While it is loaded again, the earlier value stays shown, with `refreshing`,
 * so a list does not flash to "Loading…" after each change.
 */
export type Loaded<T> =
  | { status: 'loading' }
  | { status: 'ready'; value: T; refreshing: boolean }
  | { status: 'failed'; result: Refused };

/**
 * Loads a value when `dependencies` change, or when `reload` is called. A response that arrives after a
 * newer request has been sent is ignored.
 */
export function useLoad<T>(load: (session: Session) => Promise<Result<T>>, dependencies: DependencyList) {
  const { run } = useApi();
  const [state, setState] = useState<Loaded<T>>({ status: 'loading' });
  const [version, setVersion] = useState(0);

  useEffect(() => {
    let current = true;
    setState((earlier) => (earlier.status === 'ready' ? { ...earlier, refreshing: true } : { status: 'loading' }));
    void run(load).then((result) => {
      if (current) {
        setState(result.outcome === 'ok' ? { status: 'ready', value: result.value, refreshing: false } : { status: 'failed', result });
      }
    });
    return () => {
      current = false;
    };
    // The caller names what the loader depends on, as with useEffect.
  }, [run, version, ...dependencies]);

  const reload = useCallback(() => setVersion((value) => value + 1), []);
  /** Shows a value that the API has confirmed, without waiting for the next load. */
  const set = useCallback((value: T) => setState({ status: 'ready', value, refreshing: false }), []);
  const retry = useCallback(() => {
    setState({ status: 'loading' });
    setVersion((value) => value + 1);
  }, []);
  return { state, reload, retry, set };
}

/** A submission that runs once at a time: a repeated click while it runs sends nothing (I13 scope decision 9). */
export function useSubmit() {
  const gate = useRef(new Gate());
  const [busy, setBusy] = useState(false);
  const submit = useCallback(async <R,>(work: () => Promise<R>): Promise<R | undefined> => {
    return gate.current.run(async () => {
      setBusy(true);
      try {
        return await work();
      } finally {
        setBusy(false);
      }
    });
  }, []);
  return { busy, submit };
}
