// The call to the member operations of the Matriz API (openapi.yaml). It throws `SessionEndedError`
// if the token is refused (401); the other refusals are returned, so that a screen can show them.
import { type Session, withToken } from './api';
import type { ProjectResult } from './projectsApi';

export type MemberRole = 'PROJECT_OWNER' | 'PROJECT_PARTICIPANT';

/** A member of a project (Member): the Project Owner comes first, then the others by display name. */
export interface Member {
  person: { personId: string; displayName: string };
  role: MemberRole;
}

/** `GET /api/projects/{projectId}/members` (listProjectMembers). */
export async function listProjectMembers(session: Session, projectId: string): Promise<ProjectResult<Member[]>> {
  const response = await withToken(session, `/api/projects/${encodeURIComponent(projectId)}/members`);
  if (response.ok) {
    return { outcome: 'ok', value: (await response.json()) as Member[] };
  }
  return response.status === 404 ? { outcome: 'notFound' } : { outcome: 'failed' };
}
