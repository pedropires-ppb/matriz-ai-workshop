import { type FormEvent, useState } from 'react';
import { messages } from './messages';
import { fieldMessages, type FieldMessages, refusal, validateProject } from './projectValidation';
import type { Project, ProjectResult } from './projectsApi';

interface Props {
  idPrefix: string;
  initialName: string;
  initialDescription: string;
  submitLabel: string;
  /** Sends the content; the form shows what is refused. */
  onSubmit: (content: { name: string; description: string }) => Promise<ProjectResult<Project>>;
  onSaved: (project: Project) => void;
  onCancel: () => void;
}

/** The form to create or edit a project: its name and its description, with their validation messages. */
export default function ProjectForm(props: Props) {
  const [name, setName] = useState(props.initialName);
  const [description, setDescription] = useState(props.initialDescription);
  const [problems, setProblems] = useState<FieldMessages>({});
  const [failure, setFailure] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const nameId = `${props.idPrefix}-name`;
  const descriptionId = `${props.idPrefix}-description`;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFailure(null);
    const found = validateProject(name, description);
    setProblems(found);
    if (found.name !== undefined || found.description !== undefined) {
      return;
    }
    setSubmitting(true);
    const result = await props.onSubmit({ name, description });
    setSubmitting(false);
    if (result.outcome === 'ok') {
      props.onSaved(result.value);
      return;
    }
    if (result.outcome === 'invalid') {
      setProblems(fieldMessages(result.problems));
    }
    setFailure(refusal(result));
  }

  return (
    <form noValidate onSubmit={(event) => void submit(event)}>
      <p>
        <label htmlFor={nameId}>{messages.name}</label>
        <input
          id={nameId}
          name="name"
          autoFocus
          value={name}
          aria-invalid={problems.name !== undefined}
          aria-describedby={problems.name !== undefined ? `${nameId}-error` : undefined}
          onChange={(event) => setName(event.target.value)}
        />
      </p>
      {problems.name !== undefined && (
        <p id={`${nameId}-error`} role="alert">
          {problems.name}
        </p>
      )}
      <p>
        <label htmlFor={descriptionId}>{messages.description}</label>
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
