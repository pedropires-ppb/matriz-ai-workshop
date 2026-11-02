// Calls to the project operations of the Matriz API (openapi.yaml). Each one throws
// `SessionEndedError` if the token is refused (401), which the caller handles by sending the person
// to sign in. The other refusals are returned, not thrown, so that a screen can show them.
import { type Session, withToken } from './api';

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

/** One page of My projects (ProjectPage). `page` starts at 1; `totalPages` is 0 if nothing matches. */
export interface ProjectPage {
  items: Project[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

/** A member of the content that the API refused, by its JSON Pointer (ValidationProblem). */
export interface FieldProblem {
  pointer: string;
  detail: string;
}

/** What the API did with a request that changes or reads one project. */
export type ProjectResult<T> =
  | { outcome: 'ok'; value: T }
  | { outcome: 'invalid'; problems: FieldProblem[] }
  | { outcome: 'notFound' }
  | { outcome: 'forbidden' }
  | { outcome: 'completed' }
  | { outcome: 'failed' };

const JSON_TYPE = 'application/json';
const MERGE_PATCH_TYPE = 'application/merge-patch+json';

/** `GET /api/projects` (listProjects): a page of the person's projects, by name if given. */
export async function listProjects(
  session: Session,
  query: { name: string; page: number; size: number },
): Promise<ProjectResult<ProjectPage>> {
  const parameters = new URLSearchParams({ page: String(query.page), size: String(query.size) });
  if (query.name !== '') {
    parameters.set('name', query.name);
  }
  return answer(await withToken(session, `/api/projects?${parameters.toString()}`));
}

/** `POST /api/projects` (createProject). The Project Owner is the signed-in person. */
export async function createProject(
  session: Session,
  content: { name: string; description: string },
): Promise<ProjectResult<Project>> {
  return answer(
    await withToken(session, '/api/projects', {
      method: 'POST',
      headers: { 'Content-Type': JSON_TYPE },
      body: JSON.stringify(withoutEmptyDescription(content)),
    }),
  );
}

/** `GET /api/projects/{projectId}` (getProject). */
export async function getProject(session: Session, projectId: string): Promise<ProjectResult<Project>> {
  return answer(await withToken(session, `/api/projects/${encodeURIComponent(projectId)}`));
}

/** `PATCH /api/projects/{projectId}` (updateProject): the name and the description. */
export async function updateProject(
  session: Session,
  projectId: string,
  content: { name: string; description: string },
): Promise<ProjectResult<Project>> {
  const body = { name: content.name, description: content.description.trim() === '' ? null : content.description };
  return answer(
    await withToken(session, `/api/projects/${encodeURIComponent(projectId)}`, {
      method: 'PATCH',
      headers: { 'Content-Type': MERGE_PATCH_TYPE },
      body: JSON.stringify(body),
    }),
  );
}

/** `DELETE /api/projects/{projectId}` (deleteProject). The confirmation is asked before this. */
export async function deleteProject(session: Session, projectId: string): Promise<ProjectResult<null>> {
  return answer(
    await withToken(session, `/api/projects/${encodeURIComponent(projectId)}`, { method: 'DELETE' }),
    null,
  );
}

function withoutEmptyDescription(content: { name: string; description: string }) {
  return content.description.trim() === '' ? { name: content.name } : content;
}

/** Reads the response: its content if it has one, or `empty` for an answer without content. */
async function answer<T>(response: Response, empty?: null): Promise<ProjectResult<T>> {
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
