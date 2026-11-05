// Calls to the task operations of the Matriz API (openapi.yaml). Each one throws
// `SessionEndedError` if the token is refused (401), which the caller handles by sending the person
// to sign in. The other refusals are returned, not thrown, so that a screen can show them.
import { type Session, withToken } from './api';
import type { FieldProblem, ProjectResult } from './projectsApi';

export type TaskState = 'TO_DO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export const TASK_STATES: readonly TaskState[] = ['TO_DO', 'IN_PROGRESS', 'DONE'];
export const TASK_PRIORITIES: readonly TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH'];

/** The size of a page of tasks; the contract allows 1 to 100 and defaults to 20. */
export const TASKS_PAGE_SIZE = 20;

/** A task (Task). `description` is absent if none. */
export interface Task {
  taskId: string;
  projectId: string;
  title: string;
  description?: string;
  state: TaskState;
  priority: TaskPriority;
  assignees: { personId: string; displayName: string }[];
}

/** One page of the tasks of a project (TaskPage). `page` starts at 1; `totalPages` is 0 if nothing matches. */
export interface TaskPage {
  items: Task[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

/** What the API did with a request about a task: the same outcomes as for a project. */
export type TaskResult<T> = ProjectResult<T>;
export type { FieldProblem };

const JSON_TYPE = 'application/json';
const MERGE_PATCH_TYPE = 'application/merge-patch+json';

function tasksPath(projectId: string): string {
  return `/api/projects/${encodeURIComponent(projectId)}/tasks`;
}

function taskPath(projectId: string, taskId: string): string {
  return `${tasksPath(projectId)}/${encodeURIComponent(taskId)}`;
}

/** `GET /api/projects/{projectId}/tasks` (listTasks): a page of the tasks, filtered by state and priority. */
export async function listTasks(
  session: Session,
  projectId: string,
  query: { states: readonly TaskState[]; priorities: readonly TaskPriority[]; page: number; size: number },
): Promise<TaskResult<TaskPage>> {
  const parameters = new URLSearchParams({ page: String(query.page), size: String(query.size) });
  query.states.forEach((state) => parameters.append('state', state));
  query.priorities.forEach((priority) => parameters.append('priority', priority));
  return answer(await withToken(session, `${tasksPath(projectId)}?${parameters.toString()}`));
}

/** `POST /api/projects/{projectId}/tasks` (createTask). The state starts as To do. */
export async function createTask(
  session: Session,
  projectId: string,
  content: { title: string; description: string; priority: TaskPriority },
): Promise<TaskResult<Task>> {
  const body: Record<string, string> = { title: content.title, priority: content.priority };
  if (content.description.trim() !== '') {
    body.description = content.description;
  }
  return answer(
    await withToken(session, tasksPath(projectId), {
      method: 'POST',
      headers: { 'Content-Type': JSON_TYPE },
      body: JSON.stringify(body),
    }),
  );
}

/** `PATCH /api/projects/{projectId}/tasks/{taskId}` (updateTask): only the members that are given change. */
export async function updateTask(
  session: Session,
  projectId: string,
  taskId: string,
  changes: { title?: string; description?: string; priority?: TaskPriority },
): Promise<TaskResult<Task>> {
  const body: Record<string, string | null> = {};
  if (changes.title !== undefined) {
    body.title = changes.title;
  }
  if (changes.description !== undefined) {
    body.description = changes.description.trim() === '' ? null : changes.description;
  }
  if (changes.priority !== undefined) {
    body.priority = changes.priority;
  }
  return answer(
    await withToken(session, taskPath(projectId, taskId), {
      method: 'PATCH',
      headers: { 'Content-Type': MERGE_PATCH_TYPE },
      body: JSON.stringify(body),
    }),
  );
}

/** `PUT /api/projects/{projectId}/tasks/{taskId}/state` (changeTaskState): any state to any state. */
export async function changeTaskState(
  session: Session,
  projectId: string,
  taskId: string,
  state: TaskState,
): Promise<TaskResult<Task>> {
  return answer(
    await withToken(session, `${taskPath(projectId, taskId)}/state`, {
      method: 'PUT',
      headers: { 'Content-Type': JSON_TYPE },
      body: JSON.stringify({ state }),
    }),
  );
}

/** `DELETE /api/projects/{projectId}/tasks/{taskId}` (deleteTask). The confirmation is asked before this. */
export async function deleteTask(session: Session, projectId: string, taskId: string): Promise<TaskResult<null>> {
  return answer(await withToken(session, taskPath(projectId, taskId), { method: 'DELETE' }), null);
}

/** Reads the response: its content if it has one, or `empty` for an answer without content. */
async function answer<T>(response: Response, empty?: null): Promise<TaskResult<T>> {
  if (response.ok) {
    return { outcome: 'ok', value: (empty === undefined ? await response.json() : empty) as T };
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
