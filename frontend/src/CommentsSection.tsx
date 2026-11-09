import { type FormEvent, useCallback, useEffect, useState } from 'react';
import { type Session, SessionEndedError } from './api';
import { addComment, type Comment, type CommentResult, listComments } from './commentsApi';
import { messages } from './messages';

export const COMMENT_MAXIMUM = 2000;

/**
 * Counts characters as the API does: Unicode characters of the trimmed text, not UTF-16 units.
 * `trim` also removes the no-break spaces that the API treats as blank (Assumptions A-033, A-036).
 */
function length(text: string): number {
  return Array.from(text.trim()).length;
}

/** The message for a text the API would refuse, or `null` if it is acceptable. */
export function commentProblem(text: string): string | null {
  if (length(text) === 0) {
    return messages.commentRequired;
  }
  return length(text) > COMMENT_MAXIMUM ? messages.commentTooLong : null;
}

function refusal(result: CommentResult<unknown>): string {
  switch (result.outcome) {
    case 'invalid':
      return messages.invalidComment;
    case 'notFound':
      return messages.taskNotFound;
    default:
      return messages.somethingFailed;
  }
}

/** A date and time from the server, shown in the browser's time zone. */
function shown(createdAt: string): string {
  return new Date(createdAt).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

interface Props {
  session: Session;
  projectId: string;
  taskId: string;
  onSessionEnded: () => void;
}

/**
 * The comments of a task (US-021, US-022): the list, oldest first, with each author and date as the
 * server gives them, and a form to add one. A Completed project still allows comments, so the form is
 * always shown. After adding, the list is fetched again, so nobody reloads the page. A comment is never
 * edited or deleted (Assumption A-023).
 */
export default function CommentsSection({ session, projectId, taskId, onSessionEnded }: Props) {
  const [comments, setComments] = useState<Comment[] | null>(null);
  const [loadFailed, setLoadFailed] = useState<string | null>(null);
  const [reload, setReload] = useState(0);
  const [text, setText] = useState('');
  const [problem, setProblem] = useState<string | null>(null);
  const [failure, setFailure] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const guard = useCallback(
    async <T,>(request: () => Promise<CommentResult<T>>): Promise<CommentResult<T>> => {
      try {
        return await request();
      } catch (error: unknown) {
        if (error instanceof SessionEndedError) {
          onSessionEnded();
        }
        return { outcome: 'failed' };
      }
    },
    [onSessionEnded],
  );

  useEffect(() => {
    let current = true;
    void guard(() => listComments(session, projectId, taskId)).then((result) => {
      if (!current) {
        return;
      }
      if (result.outcome === 'ok') {
        setComments(result.value);
        setLoadFailed(null);
      } else {
        setLoadFailed(refusal(result));
      }
    });
    return () => {
      current = false;
    };
  }, [guard, session, projectId, taskId, reload]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setNotice(null);
    setFailure(null);
    const found = commentProblem(text);
    setProblem(found);
    if (found !== null) {
      return;
    }
    setBusy(true);
    const result = await guard(() => addComment(session, projectId, taskId, text));
    setBusy(false);
    if (result.outcome === 'ok') {
      setText('');
      setNotice(messages.commentAdded);
      setReload((value) => value + 1);
    } else {
      setFailure(refusal(result));
    }
  }

  return (
    <section aria-labelledby="comments-heading">
      <h5 id="comments-heading">{messages.comments}</h5>
      {loadFailed !== null && <p role="alert">{loadFailed}</p>}
      {comments !== null &&
        (comments.length === 0 ? (
          <p>{messages.noComments}</p>
        ) : (
          <ul aria-label={messages.comments}>
            {comments.map((comment) => (
              <li key={comment.commentId}>
                <strong>{comment.author.displayName}</strong> ·{' '}
                <time dateTime={comment.createdAt}>{shown(comment.createdAt)}</time>
                <div style={{ whiteSpace: 'pre-wrap' }}>{comment.text}</div>
              </li>
            ))}
          </ul>
        ))}
      <form onSubmit={(event) => void submit(event)} noValidate>
        <p>
          <label htmlFor="new-comment">{messages.newComment}</label>
          <br />
          <textarea
            id="new-comment"
            rows={3}
            cols={60}
            value={text}
            disabled={busy}
            aria-invalid={problem !== null}
            aria-describedby="new-comment-count"
            onChange={(event) => {
              setText(event.target.value);
              setProblem(null);
            }}
          />
          <br />
          <span id="new-comment-count">{messages.commentCount(length(text), COMMENT_MAXIMUM)}</span>
        </p>
        {problem !== null && <p role="alert">{problem}</p>}
        {failure !== null && <p role="alert">{failure}</p>}
        {notice !== null && <p role="status">{notice}</p>}
        <button type="submit" disabled={busy}>
          {messages.addComment}
        </button>
      </form>
    </section>
  );
}
