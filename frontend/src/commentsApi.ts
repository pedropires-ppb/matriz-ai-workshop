// The comment operations of the Matriz API (openapi.yaml). The author and the date are never sent: the
// server sets them (Assumption A-036).
import { call, type PersonSummary, type Result, sending, type Session } from './http';

/** A comment (Comment). `createdAt` is the server's date and time, RFC 3339 in UTC. */
export interface Comment {
  commentId: string;
  taskId: string;
  author: PersonSummary;
  createdAt: string;
  text: string;
}

function commentsPath(projectId: string, taskId: string): string {
  return `/projects/${encodeURIComponent(projectId)}/tasks/${encodeURIComponent(taskId)}/comments`;
}

/** `GET /api/projects/{projectId}/tasks/{taskId}/comments` (listTaskComments): the whole list, oldest first. */
export function listComments(session: Session, projectId: string, taskId: string): Promise<Result<Comment[]>> {
  return call(session, commentsPath(projectId, taskId));
}

/** `POST /api/projects/{projectId}/tasks/{taskId}/comments` (addTaskComment): only the text is sent. */
export function addComment(session: Session, projectId: string, taskId: string, text: string): Promise<Result<Comment>> {
  return call(session, commentsPath(projectId, taskId), sending('POST', { text }));
}
