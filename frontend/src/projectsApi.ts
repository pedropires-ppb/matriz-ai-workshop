// The project operations of the Matriz API (openapi.yaml).
import { call, MERGE_PATCH_TYPE, type Result, sending, type Session } from './http';
import type { TaskState } from './tasksApi';
import { trimText } from './validation';

export type ProjectState = 'ACTIVE' | 'COMPLETED';
export type MemberRole = 'PROJECT_OWNER' | 'PROJECT_PARTICIPANT';

/** What a project shows in a list and when viewed. `description` is absent if none. */
interface ProjectData {
  projectId: string;
  name: string;
  description?: string;
  state: ProjectState;
  myRole: MemberRole;
}

/** The number of a project's tasks in each state, not counting deleted ones (TaskCounts). */
export interface TaskCounts {
  toDo: number;
  inProgress: number;
  done: number;
}

/** A project's progress (Progress). `percentDone` is null when the project has no tasks (EX-005). */
export interface Progress {
  taskCounts: TaskCounts;
  percentDone: number | null;
}

/** A project as listed (ProjectSummary): its own counts by state, and no percentage (FR-024). */
export interface ProjectSummary extends ProjectData {
  taskCounts: TaskCounts;
}

/** A project as viewed, created, or edited (Project), with its progress (FR-012). */
export interface Project extends ProjectData {
  progress: Progress;
}

/** The counts in the order of the task states, so that a screen lists them as the glossary does. */
export function countsByState(counts: TaskCounts): [TaskState, number][] {
  return [
    ['TO_DO', counts.toDo],
    ['IN_PROGRESS', counts.inProgress],
    ['DONE', counts.done],
  ];
}

/** A page of a list. `page` starts at 1; `totalPages` is 0 if nothing matches; `totalItems` counts every match. */
export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export type ProjectPage = Page<ProjectSummary>;

export interface ProjectContent {
  name: string;
  description: string;
}

function projectPath(projectId: string): string {
  return `/projects/${encodeURIComponent(projectId)}`;
}

/** `GET /api/projects` (listProjects): a page of the person's projects, by name if given. */
export function listProjects(
  session: Session,
  query: { name: string; page: number; size: number },
): Promise<Result<ProjectPage>> {
  const parameters = new URLSearchParams({ page: String(query.page), size: String(query.size) });
  if (query.name !== '') {
    parameters.set('name', query.name);
  }
  return call(session, `/projects?${parameters.toString()}`);
}

/** `POST /api/projects` (createProject). The Project Owner is the signed-in person. */
export function createProject(session: Session, content: ProjectContent): Promise<Result<Project>> {
  const body = trimText(content.description) === '' ? { name: content.name } : content;
  return call(session, '/projects', sending('POST', body));
}

/** `GET /api/projects/{projectId}` (getProject). */
export function getProject(session: Session, projectId: string): Promise<Result<Project>> {
  return call(session, projectPath(projectId));
}

/** `PATCH /api/projects/{projectId}` (updateProject): the name and the description. */
export function updateProject(session: Session, projectId: string, content: ProjectContent): Promise<Result<Project>> {
  const body = { name: content.name, description: trimText(content.description) === '' ? null : content.description };
  return call(session, projectPath(projectId), sending('PATCH', body, MERGE_PATCH_TYPE));
}

/**
 * `PUT /api/projects/{projectId}/state` (completeProject). The confirmation is asked before this. A `409`
 * means the project has unfinished tasks or is already Completed (I8 decision 6).
 */
export function completeProject(session: Session, projectId: string): Promise<Result<Project>> {
  return call(session, `${projectPath(projectId)}/state`, sending('PUT', { state: 'COMPLETED' }));
}

/** `DELETE /api/projects/{projectId}` (deleteProject). The confirmation is asked before this. */
export function deleteProject(session: Session, projectId: string): Promise<Result<null>> {
  return call(session, projectPath(projectId), { method: 'DELETE' }, true);
}
