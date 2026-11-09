// The member operations of the Matriz API (openapi.yaml).
import { call, type PersonSummary, type Result, type Session } from './http';
import type { MemberRole } from './projectsApi';

/** A member of a project (Member): the Project Owner comes first, then the others by display name. */
export interface Member {
  person: PersonSummary;
  role: MemberRole;
}

/** `GET /api/projects/{projectId}/members` (listProjectMembers). */
export function listProjectMembers(session: Session, projectId: string): Promise<Result<Member[]>> {
  return call(session, `/projects/${encodeURIComponent(projectId)}/members`);
}
