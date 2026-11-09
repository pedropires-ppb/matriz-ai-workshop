// The member operations of the Matriz API (openapi.yaml).
import { call, type PersonSummary, type Result, sending, type Session } from './http';
import type { MemberRole } from './projectsApi';

/** A member of a project (Member): the Project Owner comes first, then the others by display name. */
export interface Member {
  person: PersonSummary;
  role: MemberRole;
  /** Whether the member is assigned to a task that is not deleted; such a member cannot be removed (EX-007). */
  hasAssignedTasks: boolean;
}

function membersPath(projectId: string): string {
  return `/projects/${encodeURIComponent(projectId)}/members`;
}

/** `GET /api/projects/{projectId}/members` (listProjectMembers). */
export function listProjectMembers(session: Session, projectId: string): Promise<Result<Member[]>> {
  return call(session, membersPath(projectId));
}

/**
 * `GET /api/projects/{projectId}/eligible-participants` (listEligibleParticipants): every person who is not a
 * member, whose display name contains `name` when it is not blank (Assumption A-030).
 */
export function listEligibleParticipants(session: Session, projectId: string, name: string): Promise<Result<PersonSummary[]>> {
  const query = name.trim() === '' ? '' : `?${new URLSearchParams({ name: name.trim() }).toString()}`;
  return call(session, `/projects/${encodeURIComponent(projectId)}/eligible-participants${query}`);
}

/**
 * `POST /api/projects/{projectId}/members` (addParticipant). A `409` means the project is Completed or the
 * person is already a member.
 */
export function addParticipant(session: Session, projectId: string, personId: string): Promise<Result<Member>> {
  return call(session, membersPath(projectId), sending('POST', { personId }));
}

/**
 * `DELETE /api/projects/{projectId}/members/{personId}` (removeParticipant). The confirmation is asked before
 * this. A `409` means the project is Completed or the person is assigned to tasks.
 */
export function removeParticipant(session: Session, projectId: string, personId: string): Promise<Result<null>> {
  return call(session, `${membersPath(projectId)}/${encodeURIComponent(personId)}`, { method: 'DELETE' }, true);
}
