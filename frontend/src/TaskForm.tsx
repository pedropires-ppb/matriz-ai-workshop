import { type FormEvent, useState } from 'react';
import { messages } from './messages';
import { taskFieldMessages, type TaskFieldMessages, taskRefusal, validateTask } from './taskValidation';
import { TASK_PRIORITIES, type Task, type TaskPriority, type TaskResult } from './tasksApi';

interface Props {
  idPrefix: string;
  initialTitle: string;
  initialDescription: string;
  /** Present when the form also chooses the priority (creation); an edit changes it elsewhere. */
  initialPriority?: TaskPriority;
  submitLabel: string;
  /** Sends the content; the form shows what is refused. */
  onSubmit: (content: { title: string; description: string; priority: TaskPriority }) => Promise<TaskResult<Task>>;
  onSaved: (task: Task) => void;
  onCancel: () => void;
}

/** The form to create or edit a task: its title and description, and its priority when created, with their messages. */
export default function TaskForm(props: Props) {
  const [title, setTitle] = useState(props.initialTitle);
  const [description, setDescription] = useState(props.initialDescription);
  const [priority, setPriority] = useState<TaskPriority>(props.initialPriority ?? 'MEDIUM');
  const [problems, setProblems] = useState<TaskFieldMessages>({});
  const [failure, setFailure] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const titleId = `${props.idPrefix}-title`;
  const descriptionId = `${props.idPrefix}-description`;
  const priorityId = `${props.idPrefix}-priority`;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFailure(null);
    const found = validateTask(title, description);
    setProblems(found);
    if (found.title !== undefined || found.description !== undefined) {
      return;
    }
    setSubmitting(true);
    const result = await props.onSubmit({ title, description, priority });
    setSubmitting(false);
    if (result.outcome === 'ok') {
      props.onSaved(result.value);
      return;
    }
    if (result.outcome === 'invalid') {
      setProblems(taskFieldMessages(result.problems));
    }
    setFailure(taskRefusal(result));
  }

  return (
    <form noValidate onSubmit={(event) => void submit(event)}>
      <p>
        <label htmlFor={titleId}>{messages.title}</label>
        <input
          id={titleId}
          name="title"
          autoFocus
          value={title}
          aria-invalid={problems.title !== undefined}
          aria-describedby={problems.title !== undefined ? `${titleId}-error` : undefined}
          onChange={(event) => setTitle(event.target.value)}
        />
      </p>
      {problems.title !== undefined && (
        <p id={`${titleId}-error`} role="alert">
          {problems.title}
        </p>
      )}
      <p>
        <label htmlFor={descriptionId}>{messages.taskDescription}</label>
        <textarea
          id={descriptionId}
          name="description"
          rows={4}
          value={description}
          aria-invalid={problems.description !== undefined}
          aria-describedby={problems.description !== undefined ? `${descriptionId}-error` : undefined}
          onChange={(event) => setDescription(event.target.value)}
        />
      </p>
      {problems.description !== undefined && (
        <p id={`${descriptionId}-error`} role="alert">
          {problems.description}
        </p>
      )}
      {props.initialPriority !== undefined && (
        <p>
          <label htmlFor={priorityId}>{messages.priorityLabel}</label>
          <select
            id={priorityId}
            name="priority"
            value={priority}
            onChange={(event) => setPriority(event.target.value as TaskPriority)}
          >
            {TASK_PRIORITIES.map((value) => (
              <option key={value} value={value}>
                {messages.taskPriorities[value]}
              </option>
            ))}
          </select>
        </p>
      )}
      {failure !== null && <p role="alert">{failure}</p>}
      <p>
        <button type="submit" disabled={submitting}>
          {props.submitLabel}
        </button>{' '}
        <button type="button" disabled={submitting} onClick={props.onCancel}>
          {messages.cancel}
        </button>
      </p>
    </form>
  );
}
