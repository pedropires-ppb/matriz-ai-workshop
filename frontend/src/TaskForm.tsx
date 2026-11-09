import { type FormEvent, useState } from 'react';
import { Button, Field, Notice, SelectField } from './components';
import type { Result } from './http';
import { messages } from './messages';
import { useSubmit } from './session';
import { TASK_PRIORITIES, type Task, type TaskPriority } from './tasksApi';
import { checkFields, fieldMessagesFrom, type FieldMessages, hasProblems, refusal, RULES } from './validation';

const TASK_RULES = { title: RULES.taskTitle, description: RULES.taskDescription };

export interface TaskContent {
  title: string;
  description: string;
  priority: TaskPriority;
}

interface Props {
  idPrefix: string;
  initial: TaskContent;
  /** Whether the form also chooses the priority (creation); an edit changes it on its own control. */
  withPriority: boolean;
  submitLabel: string;
  /** Sends the content; the form shows what is refused and keeps what was typed. */
  onSubmit: (content: TaskContent) => Promise<Result<Task>>;
  onSaved: (task: Task) => void;
  onCancel: () => void;
}

/** The form to create or edit a task: its title and description, and its priority when created, with their messages. */
export default function TaskForm({ idPrefix, initial, withPriority, submitLabel, onSubmit, onSaved, onCancel }: Props) {
  const [content, setContent] = useState<TaskContent>(initial);
  const [problems, setProblems] = useState<FieldMessages<'title' | 'description'>>({});
  const [failure, setFailure] = useState<string | null>(null);
  const { busy, submit } = useSubmit();

  function send(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void submit(async () => {
      setFailure(null);
      const found = checkFields({ title: content.title, description: content.description }, TASK_RULES);
      setProblems(found);
      if (hasProblems(found)) {
        return;
      }
      const result = await onSubmit(content);
      if (result.outcome === 'ok') {
        onSaved(result.value);
        return;
      }
      if (result.outcome === 'invalid') {
        setProblems(fieldMessagesFrom(result.problems, TASK_RULES));
      }
      setFailure(refusal(result, 'task'));
    });
  }

  return (
    <form noValidate onSubmit={send} className="form">
      <Field
        id={`${idPrefix}-title`}
        name="title"
        label={messages.title}
        value={content.title}
        autoFocus
        error={problems.title}
        onChange={(title) => setContent({ ...content, title })}
      />
      <Field
        id={`${idPrefix}-description`}
        name="description"
        label={messages.taskDescription}
        multiline
        value={content.description}
        error={problems.description}
        onChange={(description) => setContent({ ...content, description })}
      />
      {withPriority && (
        <SelectField
          id={`${idPrefix}-priority`}
          name="priority"
          label={messages.priorityLabel}
          value={content.priority}
          options={TASK_PRIORITIES.map((value) => ({ value, label: messages.taskPriorities[value] }))}
          onChange={(priority) => setContent({ ...content, priority })}
        />
      )}
      {failure !== null && <Notice kind="error">{failure}</Notice>}
      <div className="actions">
        <Button type="submit" variant="primary" busy={busy}>
          {submitLabel}
        </Button>
        <Button disabled={busy} onClick={onCancel}>
          {messages.cancel}
        </Button>
      </div>
    </form>
  );
}
