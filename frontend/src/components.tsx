// The parts that every screen reuses, so that the forms, the states, and the messages look and behave
// the same everywhere (I13 scope decisions 8–9).
import { type ReactNode, useEffect, useId, useRef } from 'react';
import { messages } from './messages';
import { formatRoute, type Route } from './router';
import type { TaskPriority, TaskState } from './tasksApi';
import type { MemberRole, ProjectState } from './projectsApi';

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
}

export function Button({ children, onClick, type = 'button', busy = false, disabled = false, variant = 'secondary' }: ButtonProps) {
  return (
    <button
      type={type}
      className={`button ${variant}`}
      disabled={busy || disabled}
      aria-busy={busy ? true : undefined}
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
