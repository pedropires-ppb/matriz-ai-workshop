// Calls to the comment operations of the Matriz API (openapi.yaml). Each one throws
// `SessionEndedError` if the token is refused (401), which the caller handles by sending the person
// to sign in. The other refusals are returned, not thrown, so that a screen can show them. The author
// and the date are never sent: the server sets them (Assumption A-036).
import { type Session, withToken } from './api';
import type { FieldProblem, ProjectResult } from './projectsApi';

/** A comment (Comment). `createdAt` is the server's date and time, RFC 3339 in UTC. */
export interface Comment {
  commentId: string;
  taskId: string;
  author: { personId: string; displayName: string };
  createdAt: string;
  text: string;
}

export type CommentResult<T> = ProjectResult<T>;
export type { FieldProblem };

function commentsPath(projectId: string, taskId: string): string {
  return `/api/projects/${encodeURIComponent(projectId)}/tasks/${encodeURIComponent(taskId)}/comments`;
}

/** `GET /api/projects/{projectId}/tasks/{taskId}/comments` (listTaskComments): the whole list, oldest first. */
export async function listComments(
  session: Session,
  projectId: string,
  taskId: string,
): Promise<CommentResult<Comment[]>> {
  return answer(await withToken(session, commentsPath(projectId, taskId)));
}

/** `POST /api/projects/{projectId}/tasks/{taskId}/comments` (addTaskComment): only the text is sent. */
export async function addComment(
  session: Session,
  projectId: string,
  taskId: string,
  text: string,
): Promise<CommentResult<Comment>> {
  return answer(
    await withToken(session, commentsPath(projectId, taskId), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text }),
    }),
  );
}

async function answer<T>(response: Response): Promise<CommentResult<T>> {
  if (response.ok) {
    return { outcome: 'ok', value: (await response.json()) as T };
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
