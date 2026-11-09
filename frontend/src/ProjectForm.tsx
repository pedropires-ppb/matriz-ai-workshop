import { type FormEvent, useState } from 'react';
import { Button, Field, Notice } from './components';
import type { Result } from './http';
import { messages } from './messages';
import type { Project, ProjectContent } from './projectsApi';
import { useSubmit } from './session';
import { checkFields, fieldMessagesFrom, type FieldMessages, hasProblems, refusal, RULES } from './validation';

const PROJECT_RULES = { name: RULES.projectName, description: RULES.projectDescription };

interface Props {
  idPrefix: string;
  initial: ProjectContent;
  submitLabel: string;
  /** Sends the content; the form shows what is refused and keeps what was typed. */
  onSubmit: (content: ProjectContent) => Promise<Result<Project>>;
  onSaved: (project: Project) => void;
  onCancel: () => void;
}

/** The form to create or edit a project: its name and its description, with their validation messages. */
export default function ProjectForm({ idPrefix, initial, submitLabel, onSubmit, onSaved, onCancel }: Props) {
  const [content, setContent] = useState<ProjectContent>(initial);
  const [problems, setProblems] = useState<FieldMessages<keyof ProjectContent>>({});
  const [failure, setFailure] = useState<string | null>(null);
  const { busy, submit } = useSubmit();

  function send(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void submit(async () => {
      setFailure(null);
      const found = checkFields(content, PROJECT_RULES);
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
        setProblems(fieldMessagesFrom(result.problems, PROJECT_RULES));
      }
      setFailure(refusal(result, 'project'));
    });
  }

  return (
    <form noValidate onSubmit={send} className="form">
      <Field
        id={`${idPrefix}-name`}
        name="name"
        label={messages.name}
        value={content.name}
        autoFocus
        error={problems.name}
        onChange={(name) => setContent({ ...content, name })}
      />
      <Field
        id={`${idPrefix}-description`}
        name="description"
        label={messages.description}
        multiline
        value={content.description}
        error={problems.description}
        onChange={(description) => setContent({ ...content, description })}
      />
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
