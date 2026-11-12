// The task operations of the Matriz API (openapi.yaml).
import { call, MERGE_PATCH_TYPE, type PersonSummary, type Result, sending, type Session } from './http';
import type { Page } from './projectsApi';

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
  assignees: PersonSummary[];
}

export type TaskPage = Page<Task>;

export interface TaskFilters {
  states: readonly TaskState[];
  priorities: readonly TaskPriority[];
}

function tasksPath(projectId: string): string {
  return `/projects/${encodeURIComponent(projectId)}/tasks`;
}

function taskPath(projectId: string, taskId: string): string {
  return `${tasksPath(projectId)}/${encodeURIComponent(taskId)}`;
}

/** `GET /api/projects/{projectId}/tasks` (listTasks): a page of the tasks, filtered by state and priority. */
export function listTasks(
  session: Session,
  projectId: string,
  query: TaskFilters & { page: number; size: number },
): Promise<Result<TaskPage>> {
  const parameters = new URLSearchParams({ page: String(query.page), size: String(query.size) });
  query.states.forEach((state) => parameters.append('state', state));
  query.priorities.forEach((priority) => parameters.append('priority', priority));
  return call(session, `${tasksPath(projectId)}?${parameters.toString()}`);
}

/** `POST /api/projects/{projectId}/tasks` (createTask). The state starts as To do. */
export function createTask(
  session: Session,
  projectId: string,
  content: { title: string; description: string; priority: TaskPriority },
): Promise<Result<Task>> {
  const body: Record<string, string> = { title: content.title, priority: content.priority };
  if (content.description.trim() !== '') {
    body.description = content.description;
  }
  return call(session, tasksPath(projectId), sending('POST', body));
}

/** `GET /api/projects/{projectId}/tasks/{taskId}` (getTask). */
export function getTask(session: Session, projectId: string, taskId: string): Promise<Result<Task>> {
  return call(session, taskPath(projectId, taskId));
}

/** `PATCH /api/projects/{projectId}/tasks/{taskId}` (updateTask): only the members that are given change. */
export function updateTask(
  session: Session,
  projectId: string,
  taskId: string,
  changes: { title?: string; description?: string; priority?: TaskPriority },
): Promise<Result<Task>> {
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
  return call(session, taskPath(projectId, taskId), sending('PATCH', body, MERGE_PATCH_TYPE));
}

/** `PUT /api/projects/{projectId}/tasks/{taskId}/state` (changeTaskState): any state to any state. */
export function changeTaskState(
  session: Session,
  projectId: string,
  taskId: string,
  state: TaskState,
): Promise<Result<Task>> {
  return call(session, `${taskPath(projectId, taskId)}/state`, sending('PUT', { state }));
}

/** `POST /api/projects/{projectId}/tasks/{taskId}/assignees` (assignTask): a repeated assignment changes nothing. */
export function assignTask(session: Session, projectId: string, taskId: string, personId: string): Promise<Result<Task>> {
  return call(session, `${taskPath(projectId, taskId)}/assignees`, sending('POST', { personId }));
}

/** `DELETE /api/projects/{projectId}/tasks/{taskId}/assignees/{personId}` (unassignTask). */
export function unassignTask(
  session: Session,
  projectId: string,
  taskId: string,
  personId: string,
): Promise<Result<Task>> {
  return call(session, `${taskPath(projectId, taskId)}/assignees/${encodeURIComponent(personId)}`, {
    method: 'DELETE',
  });
}

/** `DELETE /api/projects/{projectId}/tasks/{taskId}` (deleteTask). The confirmation is asked before this. */
export function deleteTask(session: Session, projectId: string, taskId: string): Promise<Result<null>> {
  return call(session, taskPath(projectId, taskId), { method: 'DELETE' }, true);
}
