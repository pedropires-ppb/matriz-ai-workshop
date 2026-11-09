// The project operations of the Matriz API (openapi.yaml).
import { call, MERGE_PATCH_TYPE, type Result, sending, type Session } from './http';

export type ProjectState = 'ACTIVE' | 'COMPLETED';
export type MemberRole = 'PROJECT_OWNER' | 'PROJECT_PARTICIPANT';

/** A project as listed (ProjectSummary) or viewed (Project). `description` is absent if none. */
export interface Project {
  projectId: string;
  name: string;
  description?: string;
  state: ProjectState;
  myRole: MemberRole;
}

/** A page of a list. `page` starts at 1; `totalPages` is 0 if nothing matches; `totalItems` counts every match. */
export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export type ProjectPage = Page<Project>;

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
  const body = content.description.trim() === '' ? { name: content.name } : content;
  return call(session, '/projects', sending('POST', body));
}

/** `GET /api/projects/{projectId}` (getProject). */
export function getProject(session: Session, projectId: string): Promise<Result<Project>> {
  return call(session, projectPath(projectId));
}

/** `PATCH /api/projects/{projectId}` (updateProject): the name and the description. */
export function updateProject(session: Session, projectId: string, content: ProjectContent): Promise<Result<Project>> {
  const body = { name: content.name, description: content.description.trim() === '' ? null : content.description };
  return call(session, projectPath(projectId), sending('PATCH', body, MERGE_PATCH_TYPE));
}

/** `DELETE /api/projects/{projectId}` (deleteProject). The confirmation is asked before this. */
export function deleteProject(session: Session, projectId: string): Promise<Result<null>> {
  return call(session, projectPath(projectId), { method: 'DELETE' }, true);
}
