// The parts that every screen reuses, so that the forms, the states, and the messages look and behave
// the same everywhere (I13 scope decisions 8–9).
import { type ReactNode, useEffect, useId, useRef } from 'react';
import { messages } from './messages';
import { FIRST_PAGE, formatRoute, HOME, type Route } from './router';
import type { TaskPriority, TaskState } from './tasksApi';
import { countsByState, type MemberRole, type Progress, type ProjectState, type ProjectSummary, type TaskCounts } from './projectsApi';

/** A link to a screen; it opens in the same tab, and the address can be copied or opened in a new one. */
export function Link({ to, children, className }: { to: Route; children: ReactNode; className?: string }) {
  return (
    <a href={formatRoute(to)} className={className}>
      {children}
    </a>
  );
}

interface FieldProps {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
  hint?: string;
  multiline?: boolean;
  autoFocus?: boolean;
  disabled?: boolean;
  name?: string;
  type?: string;
  autoComplete?: string;
}

/** A labelled text field, with its message under it when the text breaks a rule. */
export function Field(props: FieldProps) {
  const errorId = `${props.id}-error`;
  const hintId = `${props.id}-hint`;
  const described = [props.hint !== undefined ? hintId : null, props.error !== undefined ? errorId : null]
    .filter((id) => id !== null)
    .join(' ');
  const shared = {
    id: props.id,
    name: props.name,
    value: props.value,
    autoFocus: props.autoFocus,
    disabled: props.disabled,
    'aria-invalid': props.error !== undefined,
    'aria-describedby': described === '' ? undefined : described,
  };
  return (
    <div className="field">
      <label htmlFor={props.id}>{props.label}</label>
      {props.multiline === true ? (
        <textarea {...shared} rows={4} onChange={(event) => props.onChange(event.target.value)} />
      ) : (
        <input
          {...shared}
          type={props.type ?? 'text'}
          autoComplete={props.autoComplete}
          onChange={(event) => props.onChange(event.target.value)}
        />
      )}
      {props.hint !== undefined && (
        <span id={hintId} className="hint">
          {props.hint}
        </span>
      )}
      {props.error !== undefined && (
        <p id={errorId} role="alert" className="field-error">
          {props.error}
        </p>
      )}
    </div>
  );
}

interface SelectFieldProps<T extends string> {
  id: string;
  label: string;
  value: T | '';
  options: readonly { value: T | ''; label: string }[];
  onChange: (value: T) => void;
  disabled?: boolean;
  name?: string;
}

/** A labelled choice from a short list. */
export function SelectField<T extends string>(props: SelectFieldProps<T>) {
  return (
    <div className="field">
      <label htmlFor={props.id}>{props.label}</label>
      <select
        id={props.id}
        name={props.name}
        value={props.value}
        disabled={props.disabled}
        onChange={(event) => props.onChange(event.target.value as T)}
      >
        {props.options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </div>
  );
}

interface ButtonProps {
  children: ReactNode;
  onClick?: () => void;
  type?: 'button' | 'submit';
  /** While a request is in progress: the button cannot be pressed again. */
  busy?: boolean;
  disabled?: boolean;
  variant?: 'primary' | 'secondary' | 'danger';
  /** The identifier of a text that explains the button, for example why it is unavailable. */
  describedBy?: string;
}

export function Button({
  children,
  onClick,
  type = 'button',
  busy = false,
  disabled = false,
  variant = 'secondary',
  describedBy,
}: ButtonProps) {
  return (
    <button
      type={type}
      className={`button ${variant}`}
      disabled={busy || disabled}
      aria-busy={busy ? true : undefined}
      aria-describedby={describedBy}
      onClick={onClick}
    >
      {children}
    </button>
  );
}

/** A success notice (announced politely) or an error (announced at once). */
export function Notice({ kind, children }: { kind: 'success' | 'error'; children: ReactNode }) {
  return kind === 'success' ? (
    <p role="status" className="notice success">
      {children}
    </p>
  ) : (
    <p role="alert" className="notice error">
      {children}
    </p>
  );
}

export function Loading() {
  return (
    <p role="status" className="loading">
      {messages.loading}
    </p>
  );
}

/** A list or record that could not be loaded, with the way to try again. */
export function LoadFailed({ text, onRetry }: { text: string; onRetry: () => void }) {
  return (
    <div role="alert" className="notice error">
      <p>{text}</p>
      <Button onClick={onRetry}>{messages.retry}</Button>
    </div>
  );
}

/** An empty list, and what the person can do next. */
export function EmptyState({ text, children }: { text: string; children?: ReactNode }) {
  return (
    <div className="empty">
      <p>{text}</p>
      {children}
    </div>
  );
}

export function Pagination({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (page: number) => void }) {
  if (totalPages <= 1) {
    return null;
  }
  return (
    <nav aria-label={messages.pages} className="pagination">
      <Button disabled={page <= 1} onClick={() => onPage(page - 1)}>
        {messages.previous}
      </Button>
      <span>{messages.pageOf(page, totalPages)}</span>
      <Button disabled={page >= totalPages} onClick={() => onPage(page + 1)}>
        {messages.next}
      </Button>
    </nav>
  );
}

type Badge =
  | { kind: 'project'; value: ProjectState }
  | { kind: 'role'; value: MemberRole }
  | { kind: 'task'; value: TaskState }
  | { kind: 'priority'; value: TaskPriority };

function badgeText(badge: Badge): string {
  switch (badge.kind) {
    case 'project':
      return messages.states[badge.value];
    case 'role':
      return messages.roles[badge.value];
    case 'task':
      return messages.taskStates[badge.value];
    case 'priority':
      return messages.taskPriorities[badge.value];
  }
}

/** A state, a role, or a priority, as text; the colour only repeats the text. */
export function StateBadge(badge: Badge) {
  return <span className={`badge ${badge.kind} ${badge.value.toLowerCase()}`}>{badgeText(badge)}</span>;
}

interface ConfirmProps {
  text: string;
  confirmLabel: string;
  busy: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

/** Asks before an action that cannot be undone; Cancel has the focus, so Enter does not confirm by accident. */
export function ConfirmBox({ text, confirmLabel, busy, onConfirm, onCancel }: ConfirmProps) {
  const cancel = useRef<HTMLButtonElement>(null);
  const textId = useId();
  useEffect(() => {
    cancel.current?.focus();
  }, []);
  return (
    <div role="alertdialog" aria-labelledby={textId} aria-modal="false" className="confirm">
      <p id={textId}>{text}</p>
      <div className="actions">
        <Button variant="danger" busy={busy} onClick={onConfirm}>
          {confirmLabel}
        </Button>
        <button type="button" className="button secondary" ref={cancel} disabled={busy} onClick={onCancel}>
          {messages.cancel}
        </button>
      </div>
    </div>
  );
}

/** A project or task that cannot be shown: it does not exist, or the person is not a member (I13 decision 4). */
export function NotAccessible({ text }: { text: string }) {
  return (
    <section className="screen">
      <Notice kind="error">{text}</Notice>
      <p>
        <Link to={HOME}>{messages.goHome}</Link>
      </p>
    </section>
  );
}

/** A project's task counts by state, for example "2 To do, 1 In progress, 1 Done" (I5 decision 5). */
export function TaskCountsText({ counts }: { counts: TaskCounts }) {
  return (
    <p className="secondary-text task-counts">
      {messages.taskCounts(countsByState(counts).map(([state, count]) => [messages.taskStates[state], count]))}
    </p>
  );
}

/**
 * A project in a list, on Home and on Projects (I5 decision 4): its name, linking to its detail, its state,
 * the person's role, and its own task counts by state, with no percentage (FR-024).
 */
export function ProjectRow({ project, withDescription = false }: { project: ProjectSummary; withDescription?: boolean }) {
  return (
    <li>
      <Link to={{ screen: 'project', projectId: project.projectId, list: FIRST_PAGE }}>{project.name}</Link>
      <span className="badges">
        <StateBadge kind="project" value={project.state} />
        <StateBadge kind="role" value={project.myRole} />
      </span>
      <TaskCountsText counts={project.taskCounts} />
      {withDescription && project.description !== undefined && <p className="secondary-text">{project.description}</p>}
    </li>
  );
}

/**
 * A project's progress, as wireframe marker 4 draws it (US-006; I5 decision 3): the percentage Done with a
 * bar that repeats it, and the counts by state. With no tasks, "No tasks", with no percentage and no bar
 * (EX-005).
 */
export function ProgressPanel({ progress }: { progress: Progress }) {
  const headingId = useId();
  const { percentDone, taskCounts } = progress;
  return (
    <section aria-labelledby={headingId} className="panel project-progress">
      <h3 id={headingId}>{messages.progress}</h3>
      {percentDone === null ? (
        <p className="progress-figure">{messages.progressNoTasks}</p>
      ) : (
        <div className="progress-summary">
          <p className="progress-figure">{messages.percentDone(percentDone)}</p>
          <progress max={100} value={percentDone} aria-label={messages.progress}>
            {messages.percentDone(percentDone)}
          </progress>
        </div>
      )}
      <TaskCountsText counts={taskCounts} />
    </section>
  );
}
