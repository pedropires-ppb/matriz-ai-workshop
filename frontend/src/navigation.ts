// Moving between the screens: the current route, kept in the address, and a one-time notice shown on
// the screen that a change leads to (for example "The project has been deleted." on Projects).
import { useEffect, useState } from 'react';
import { formatRoute, parseRoute, type Route } from './router';

/** A success notice for a screen, given by the change that led to it; each one is a new object. */
export interface Flash {
  text: string;
}

export interface Location {
  route: Route;
  notice: Flash | null;
}

let pending: { hash: string; text: string } | null = null;
const listeners = new Set<() => void>();

function current(): Location {
  const hash = window.location.hash === '' ? '#/' : window.location.hash;
  const notice = pending !== null && pending.hash === hash ? { text: pending.text } : null;
  pending = null;
  return { route: parseRoute(hash), notice };
}

/** Opens a screen. `replace` leaves no entry in the history (used for filters and corrections). */
export function go(route: Route, options: { replace?: boolean; notice?: string } = {}): void {
  const hash = formatRoute(route);
  pending = options.notice === undefined ? null : { hash, text: options.notice };
  if (window.location.hash === hash) {
    listeners.forEach((listener) => listener());
  } else if (options.replace === true) {
    window.location.replace(hash);
  } else {
    window.location.hash = hash;
  }
}

function sameScreen(one: Route, other: Route): boolean {
  if (one.screen !== other.screen) {
    return false;
  }
  if (one.screen === 'project' && other.screen === 'project') {
    return one.projectId === other.projectId;
  }
  if (one.screen === 'task' && other.screen === 'task') {
    return one.projectId === other.projectId && one.taskId === other.taskId;
  }
  return true;
}

/** The current screen; it changes with the address, and the page scrolls to the top on another screen. */
export function useLocation(): Location {
  const [location, setLocation] = useState<Location>(current);

  useEffect(() => {
    function changed() {
      const next = current();
      setLocation((earlier) => {
        if (!sameScreen(earlier.route, next.route)) {
          window.scrollTo(0, 0);
        }
        return next;
      });
    }
    listeners.add(changed);
    window.addEventListener('hashchange', changed);
    return () => {
      listeners.delete(changed);
      window.removeEventListener('hashchange', changed);
    };
  }, []);

  return location;
}

/** The success notice that a screen shows: the one it was opened with, until the screen shows another. */
export function useFlash(flash: Flash | null): [string | null, (text: string | null) => void] {
  const [text, setText] = useState<string | null>(flash?.text ?? null);
  useEffect(() => {
    if (flash !== null) {
      setText(flash.text);
    }
  }, [flash]);
  return [text, setText];
}
