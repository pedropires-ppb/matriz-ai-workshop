import { type FormEvent, useState } from 'react';
import { Button, EmptyState, Field, LoadFailed, Loading, Notice } from './components';
import { addComment, type Comment, listComments } from './commentsApi';
import { messages } from './messages';
import { useApi, useLoad, useSubmit } from './session';
import { checkText, refusal, RULES, textLength } from './validation';

/** A date and time from the server, shown in the browser's time zone. */
function shown(createdAt: string): string {
  return new Date(createdAt).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

interface Props {
  projectId: string;
  taskId: string;
}

/**
 * The comments of a task (US-021, US-022): the list, oldest first, with each author and date as the
 * server gives them, and a form to add one. A Completed project still allows comments, so the form is
 * always shown. The added comment is shown from the API's answer, and the list is then fetched again. A
 * comment is never edited or deleted (Assumption A-023).
 */
export default function CommentsSection({ projectId, taskId }: Props) {
  const { run } = useApi();
  const comments = useLoad((session) => listComments(session, projectId, taskId), [projectId, taskId]);
  const [text, setText] = useState('');
  const [problem, setProblem] = useState<string | undefined>(undefined);
  const [failure, setFailure] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const { busy, submit } = useSubmit();

  function send(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void submit(async () => {
      setNotice(null);
      setFailure(null);
      const found = checkText(text, RULES.comment);
      setProblem(found);
      if (found !== undefined) {
        return;
      }
      const result = await run((session) => addComment(session, projectId, taskId, text));
      if (result.outcome === 'ok') {
        const added: Comment = result.value;
        if (comments.state.status === 'ready' && !comments.state.value.some((one) => one.commentId === added.commentId)) {
          comments.set([...comments.state.value, added]);
        }
        setText('');
        setNotice(messages.commentAdded);
        comments.reload();
      } else {
        setFailure(refusal(result, 'comment'));
      }
    });
  }

  return (
    <section aria-labelledby="comments-heading" className="panel">
      <h3 id="comments-heading">{messages.comments}</h3>
      {comments.state.status === 'loading' && <Loading />}
      {comments.state.status === 'failed' && (
        <LoadFailed text={refusal(comments.state.result, 'comment')} onRetry={comments.retry} />
      )}
      {comments.state.status === 'ready' &&
        (comments.state.value.length === 0 ? (
          <EmptyState text={messages.noComments} />
        ) : (
          <ul aria-label={messages.comments} className="rows comments">
            {comments.state.value.map((comment) => (
              <li key={comment.commentId}>
                <strong>{comment.author.displayName}</strong> ·{' '}
                <time dateTime={comment.createdAt}>{shown(comment.createdAt)}</time>
                <div className="comment-text">{comment.text}</div>
              </li>
            ))}
          </ul>
        ))}
      <form onSubmit={send} noValidate className="form">
        <Field
          id="new-comment"
          label={messages.newComment}
          multiline
          value={text}
          disabled={busy}
          error={problem}
          hint={messages.commentCount(textLength(text), RULES.comment.maximum)}
          onChange={(value) => {
            setText(value);
            setProblem(undefined);
          }}
        />
        {failure !== null && <Notice kind="error">{failure}</Notice>}
        {notice !== null && <Notice kind="success">{notice}</Notice>}
        <div className="actions">
          <Button type="submit" variant="primary" busy={busy}>
            {messages.addComment}
          </Button>
        </div>
      </form>
    </section>
  );
}
