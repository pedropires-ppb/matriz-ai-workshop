// The screens' addresses (I13 decisions 3 and 4; ADR-009, amended on 2026-11-09). The address is the part
// of the URL after `#`, so the server always serves the same page and a reload keeps the address. The
// token is kept in memory only, so a reload still signs the person out (ADR-005); after signing in again,
// the person returns to the address.
import { TASK_PRIORITIES, TASK_STATES, type TaskPriority, type TaskState } from './tasksApi';
import { trimText } from './validation';

/** The filters and the page of a project's task list, kept in the address. */
export interface TaskListQuery {
  states: TaskState[];
  priorities: TaskPriority[];
  page: number;
}

export type Route =
  | { screen: 'home' }
  | { screen: 'projects'; name: string; page: number; creating: boolean }
  | { screen: 'project'; projectId: string; list: TaskListQuery }
  | { screen: 'task'; projectId: string; taskId: string; list: TaskListQuery }
  | { screen: 'unknown' };

export type ProjectsRoute = Extract<Route, { screen: 'projects' }>;

export const HOME: Route = { screen: 'home' };
export const ALL_PROJECTS: ProjectsRoute = { screen: 'projects', name: '', page: 1, creating: false };
export const NEW_PROJECT: ProjectsRoute = { ...ALL_PROJECTS, creating: true };
export const FIRST_PAGE: TaskListQuery = { states: [], priorities: [], page: 1 };

/** A page number from the address; anything that is not a whole number of at least 1 is page 1. */
function pageFrom(parameters: URLSearchParams): number {
  const text = parameters.get('page') ?? '';
  return /^[1-9][0-9]{0,8}$/.test(text) ? Number(text) : 1;
}

/** The values of a repeated parameter that are known, each once, in their canonical order. */
function known<T extends string>(parameters: URLSearchParams, name: string, values: readonly T[]): T[] {
  const given = parameters.getAll(name);
  return values.filter((value) => given.includes(value));
}

function listFrom(parameters: URLSearchParams): TaskListQuery {
  return {
    states: known(parameters, 'state', TASK_STATES),
    priorities: known(parameters, 'priority', TASK_PRIORITIES),
    page: pageFrom(parameters),
  };
}

function segment(text: string): string | null {
  try {
    const decoded = decodeURIComponent(text);
    return decoded === '' ? null : decoded;
  } catch {
    return null;
  }
}

/** The screen that an address such as `#/projects/{id}?state=DONE` names. */
export function parseRoute(hash: string): Route {
  const address = hash.startsWith('#') ? hash.slice(1) : hash;
  const queryStart = address.indexOf('?');
  const path = queryStart === -1 ? address : address.slice(0, queryStart);
  const parameters = new URLSearchParams(queryStart === -1 ? '' : address.slice(queryStart + 1));
  const parts = path.split('/').filter((part) => part !== '');

  if (parts.length === 0) {
    return HOME;
  }
  if (parts[0] !== 'projects') {
    return { screen: 'unknown' };
  }
  if (parts.length === 1) {
    return { screen: 'projects', name: trimText(parameters.get('name') ?? ''), page: pageFrom(parameters), creating: false };
  }
  if (parts.length === 2 && parts[1] === 'new') {
    return NEW_PROJECT;
  }
  const projectId = segment(parts[1]);
  if (projectId === null) {
    return { screen: 'unknown' };
  }
  if (parts.length === 2) {
    return { screen: 'project', projectId, list: listFrom(parameters) };
  }
  const taskId = parts.length === 4 && parts[2] === 'tasks' ? segment(parts[3]) : null;
  if (taskId === null) {
    return { screen: 'unknown' };
  }
  return { screen: 'task', projectId, taskId, list: listFrom(parameters) };
}

function listQuery(list: TaskListQuery): URLSearchParams {
  const parameters = new URLSearchParams();
  list.states.forEach((state) => parameters.append('state', state));
  list.priorities.forEach((priority) => parameters.append('priority', priority));
  if (list.page > 1) {
    parameters.set('page', String(list.page));
  }
  return parameters;
}

function withQuery(path: string, parameters: URLSearchParams): string {
  const query = parameters.toString();
  return query === '' ? `#${path}` : `#${path}?${query}`;
}

/** The address of a screen; `parseRoute(formatRoute(route))` gives the same screen back. */
export function formatRoute(route: Route): string {
  switch (route.screen) {
    case 'home':
    case 'unknown':
      return '#/';
    case 'projects': {
      if (route.creating) {
        return '#/projects/new';
      }
      const parameters = new URLSearchParams();
      if (route.name !== '') {
        parameters.set('name', route.name);
      }
      if (route.page > 1) {
        parameters.set('page', String(route.page));
      }
      return withQuery('/projects', parameters);
    }
    case 'project':
      return withQuery(`/projects/${encodeURIComponent(route.projectId)}`, listQuery(route.list));
    case 'task':
      return withQuery(
        `/projects/${encodeURIComponent(route.projectId)}/tasks/${encodeURIComponent(route.taskId)}`,
        listQuery(route.list),
      );
  }
}
