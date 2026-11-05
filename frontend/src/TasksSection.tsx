import { useCallback, useEffect, useRef, useState } from 'react';
import { type Session, SessionEndedError } from './api';
import type { Member } from './membersApi';
import { messages } from './messages';
import TaskForm from './TaskForm';
import { taskRefusal } from './taskValidation';
import {
  assignTask,
  changeTaskState,
  createTask,
  deleteTask,
  listTasks,
  TASK_PRIORITIES,
  TASK_STATES,
  TASKS_PAGE_SIZE,
  type Task,
  type TaskPage,
  type TaskPriority,
  type TaskResult,
  type TaskState,
  unassignTask,
  updateTask,
} from './tasksApi';

interface Props {
  session: Session;
  projectId: string;
  /** Whether the person may create, edit, and delete tasks: the Project Owner of an Active project. */
  canChange: boolean;
  isOwner: boolean;
  /** Whether the project is Active; a Completed project allows no change to a task. */
  projectActive: boolean;
  /** The members of the project, from which the Project Owner picks who to assign; `null` until fetched. */
  members: Member[] | null;
  /** Why the person may not change them, shown instead of the controls. */
  readOnlyText: string | null;
  onSessionEnded: () => void;
}

type Load = { status: 'loading' } | { status: 'ready'; page: TaskPage } | { status: 'failed'; text: string };

function toggled<T>(values: readonly T[], value: T): T[] {
  return values.includes(value) ? values.filter((one) => one !== value) : [...values, value];
}

/**
 * The tasks of a project (US-009 to US-013, US-017): the list with its filters and pages, the
 * creation, the details with the state and priority controls, the editing, and the deletion after
 * a confirmation. After each operation the page is fetched again, so the list and the count are
 * those of the server and nobody reloads the page.
 */
export default function TasksSection({
  session,
  projectId,
  canChange,
  isOwner,
  projectActive,
  members,
  readOnlyText,
  onSessionEnded,
}: Props) {
  const [states, setStates] = useState<TaskState[]>([]);
  const [priorities, setPriorities] = useState<TaskPriority[]>([]);
  const [page, setPage] = useState(1);
  const [reload, setReload] = useState(0);
  const [load, setLoad] = useState<Load>({ status: 'loading' });
  const [creating, setCreating] = useState(false);
  const [selected, setSelected] = useState<Task | null>(null);
  const [editing, setEditing] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [failure, setFailure] = useState<string | null>(null);
  const [personToAssign, setPersonToAssign] = useState('');
  const cancelButton = useRef<HTMLButtonElement>(null);

  /** Runs a request; a 401 sends the person to sign in. */
  const guard = useCallback(
    async <T,>(request: () => Promise<TaskResult<T>>): Promise<TaskResult<T>> => {
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
    void guard(() => listTasks(session, projectId, { states, priorities, page, size: TASKS_PAGE_SIZE })).then(
      (result) => {
        if (!current) {
          return;
        }
        if (result.outcome !== 'ok') {
          setLoad({ status: 'failed', text: taskRefusal(result) ?? messages.somethingFailed });
        } else if (result.value.items.length === 0 && page > 1 && result.value.totalPages > 0) {
          setPage(result.value.totalPages);
        } else if (result.value.items.length === 0 && page > 1) {
          setPage(1);
        } else {
          setLoad({ status: 'ready', page: result.value });
        }
      },
    );
    return () => {
      current = false;
    };
  }, [guard, session, projectId, states, priorities, page, reload]);

  useEffect(() => {
    if (confirming) {
      cancelButton.current?.focus();
    }
  }, [confirming]);

  const total = load.status === 'ready' ? load.page.totalItems : 0;

  function refetch() {
    setReload((value) => value + 1);
  }

  function select(task: Task | null) {
    setSelected(task);
    setEditing(false);
    setConfirming(false);
    setPersonToAssign('');
    setNotice(null);
    setFailure(null);
  }

  /** Shows a refusal; a task that is gone from the server is no longer shown as selected. */
  function refused(result: TaskResult<unknown>, texts: Partial<Record<TaskResult<unknown>['outcome'], string>> = {}) {
    setFailure(texts[result.outcome] ?? taskRefusal(result));
    setConfirming(false);
    if (result.outcome === 'notFound') {
      setSelected(null);
    }
    refetch();
  }

  async function change<T>(
    request: () => Promise<TaskResult<T>>,
    saved: (value: T) => void,
    texts?: Partial<Record<TaskResult<unknown>['outcome'], string>>,
  ) {
    setBusy(true);
    setNotice(null);
    setFailure(null);
    const result = await guard(request);
    setBusy(false);
    if (result.outcome === 'ok') {
      saved(result.value);
      refetch();
    } else {
      refused(result, texts);
    }
  }

  function changeStateTo(task: Task, state: TaskState) {
    void change(
      () => changeTaskState(session, projectId, task.taskId, state),
      (changed) => setSelected(changed),
      { forbidden: messages.stateForbidden },
    );
  }

  function assign(task: Task, personId: string) {
    void change(
      () => assignTask(session, projectId, task.taskId, personId),
      (changed) => {
        setSelected(changed);
        setPersonToAssign('');
        setNotice(messages.assigneeAdded);
      },
      { forbidden: messages.assignmentForbidden, invalid: messages.notMemberOfProject },
    );
  }

  function unassign(task: Task, personId: string) {
    void change(
      () => unassignTask(session, projectId, task.taskId, personId),
      (changed) => {
        setSelected(changed);
        setNotice(messages.assigneeRemoved);
      },
      { forbidden: messages.assignmentForbidden },
    );
  }

  function changePriorityTo(task: Task, priority: TaskPriority) {
    void change(
      () => updateTask(session, projectId, task.taskId, { priority }),
      (changed) => setSelected(changed),
    );
  }

  function confirmDelete(task: Task) {
    void change(
      () => deleteTask(session, projectId, task.taskId),
      () => {
        setSelected(null);
        setConfirming(false);
        setNotice(messages.taskDeleted);
      },
    );
  }

  function filterChanged() {
    setPage(1);
    select(null);
  }

  const filtered = states.length > 0 || priorities.length > 0;
  const me = session.person.personId;
  const assignedToMe = selected?.assignees.some((person) => person.personId === me) ?? false;
  const canChangeState = projectActive && (isOwner || assignedToMe);
  const candidates = (members ?? []).filter(
    (member) => !(selected?.assignees ?? []).some((person) => person.personId === member.person.personId),
  );

  return (
    <section aria-labelledby="tasks-heading">
      <h3 id="tasks-heading">{messages.tasks}</h3>
      <div role="group" aria-label={messages.filterByState}>
        <span>{messages.filterByState}:</span>{' '}
        {TASK_STATES.map((state) => (
          <label key={state}>
            <input
              type="checkbox"
              checked={states.includes(state)}
              onChange={() => {
                setStates(toggled(states, state));
                filterChanged();
              }}
            />{' '}
            {messages.taskStates[state]}{' '}
          </label>
        ))}
      </div>
      <div role="group" aria-label={messages.filterByPriority}>
        <span>{messages.filterByPriority}:</span>{' '}
        {TASK_PRIORITIES.map((priority) => (
          <label key={priority}>
            <input
              type="checkbox"
              checked={priorities.includes(priority)}
              onChange={() => {
                setPriorities(toggled(priorities, priority));
                filterChanged();
              }}
            />{' '}
            {messages.taskPriorities[priority]}{' '}
          </label>
        ))}
      </div>
      {filtered && (
        <p>
          <button
            type="button"
            onClick={() => {
              setStates([]);
              setPriorities([]);
              filterChanged();
            }}
          >
            {messages.clearFilters}
          </button>
        </p>
      )}
      {readOnlyText !== null && <p>{readOnlyText}</p>}
      {canChange && !creating && (
        <p>
          <button
            type="button"
            onClick={() => {
              select(null);
              setCreating(true);
            }}
          >
            {messages.newTask}
          </button>
        </p>
      )}
      {creating && (
        <TaskForm
          idPrefix="new-task"
          initialTitle=""
          initialDescription=""
          initialPriority="MEDIUM"
          submitLabel={messages.createTask}
          onSubmit={(content) => guard(() => createTask(session, projectId, content))}
          onSaved={(task) => {
            setCreating(false);
            setSelected(task);
            setFailure(null);
            setNotice(messages.taskCreated);
            setPage(Math.max(1, Math.ceil((total + 1) / TASKS_PAGE_SIZE)));
            refetch();
          }}
          onCancel={() => setCreating(false)}
        />
      )}
      {notice !== null && <p role="status">{notice}</p>}
      {failure !== null && <p role="alert">{failure}</p>}
      {load.status === 'loading' && <p>{messages.loading}</p>}
      {load.status === 'failed' && <p role="alert">{load.text}</p>}
      {load.status === 'ready' && (
        <>
          <p id="task-count" aria-live="polite">
            {messages.taskCount(load.page.totalItems)}
          </p>
          {load.page.totalItems === 0 ? (
            <p>{filtered ? messages.noTaskMatches : messages.noTasks}</p>
          ) : (
            <ul aria-label={messages.tasks}>
              {load.page.items.map((task) => (
                <li key={task.taskId}>
                  <button
                    type="button"
                    aria-current={selected?.taskId === task.taskId ? 'true' : undefined}
                    onClick={() => select(task)}
                  >
                    {task.title}
                  </button>{' '}
                  <span>{messages.taskStates[task.state]}</span> · <span>{messages.taskPriorities[task.priority]}</span>
                </li>
              ))}
            </ul>
          )}
          {load.page.totalPages > 1 && (
            <p>
              <button type="button" disabled={page <= 1} onClick={() => setPage(page - 1)}>
                {messages.previous}
              </button>{' '}
              <span>{messages.pageOf(load.page.page, load.page.totalPages)}</span>{' '}
              <button type="button" disabled={page >= load.page.totalPages} onClick={() => setPage(page + 1)}>
                {messages.next}
              </button>
            </p>
          )}
        </>
      )}
      {selected !== null && (
        <section aria-labelledby="task-heading">
          <h4 id="task-heading">{selected.title}</h4>
          {editing ? (
            <TaskForm
              idPrefix="edit-task"
              initialTitle={selected.title}
              initialDescription={selected.description ?? ''}
              submitLabel={messages.saveChanges}
              onSubmit={(content) =>
                guard(() =>
                  updateTask(session, projectId, selected.taskId, {
                    title: content.title,
                    description: content.description,
                  }),
                )
              }
              onSaved={(task) => {
                setSelected(task);
                setEditing(false);
                setFailure(null);
                setNotice(messages.taskSaved);
                refetch();
              }}
              onCancel={() => setEditing(false)}
            />
          ) : (
            <>
              <p>{selected.description ?? messages.noDescription}</p>
              {canChangeState || canChange ? (
                <p>
                  <label htmlFor="task-state">{messages.stateLabel}</label>{' '}
                  <select
                    id="task-state"
                    value={selected.state}
                    disabled={busy || !canChangeState}
                    onChange={(event) => changeStateTo(selected, event.target.value as TaskState)}
                  >
                    {TASK_STATES.map((state) => (
                      <option key={state} value={state}>
                        {messages.taskStates[state]}
                      </option>
                    ))}
                  </select>{' '}
                  <label htmlFor="task-priority">{messages.priorityLabel}</label>{' '}
                  <select
                    id="task-priority"
                    value={selected.priority}
                    disabled={busy || !canChange}
                    onChange={(event) => changePriorityTo(selected, event.target.value as TaskPriority)}
                  >
                    {TASK_PRIORITIES.map((priority) => (
                      <option key={priority} value={priority}>
                        {messages.taskPriorities[priority]}
                      </option>
                    ))}
                  </select>
                </p>
              ) : (
                <dl>
                  <dt>{messages.stateLabel}</dt>
                  <dd>{messages.taskStates[selected.state]}</dd>
                  <dt>{messages.priorityLabel}</dt>
                  <dd>{messages.taskPriorities[selected.priority]}</dd>
                </dl>
              )}
              <div role="group" aria-label={messages.assigneesLabel}>
                <p>{messages.assigneesLabel}:</p>
                {selected.assignees.length === 0 ? (
                  <p>{messages.noAssignees}</p>
                ) : (
                  <ul aria-label={messages.assigneesLabel}>
                    {selected.assignees.map((person) => (
                      <li key={person.personId}>
                        {person.displayName}
                        {projectActive && (isOwner || person.personId === me) && (
                          <>
                            {' '}
                            <button type="button" disabled={busy} onClick={() => unassign(selected, person.personId)}>
                              {isOwner ? messages.unassign(person.displayName) : messages.unassignMe}
                            </button>
                          </>
                        )}
                      </li>
                    ))}
                  </ul>
                )}
                {projectActive && isOwner && (
                  <p>
                    {candidates.length === 0 ? (
                      messages.allMembersAssigned
                    ) : (
                      <>
                        <label htmlFor="task-assignee">{messages.personToAssign}</label>{' '}
                        <select
                          id="task-assignee"
                          value={personToAssign}
                          disabled={busy}
                          onChange={(event) => setPersonToAssign(event.target.value)}
                        >
                          <option value="">{messages.chooseMember}</option>
                          {candidates.map((member) => (
                            <option key={member.person.personId} value={member.person.personId}>
                              {member.person.displayName}
                            </option>
                          ))}
                        </select>{' '}
                        <button
                          type="button"
                          disabled={busy || personToAssign === ''}
                          onClick={() => assign(selected, personToAssign)}
                        >
                          {messages.assign}
                        </button>
                      </>
                    )}
                  </p>
                )}
                {projectActive && !isOwner && !assignedToMe && (
                  <p>
                    <button type="button" disabled={busy} onClick={() => assign(selected, me)}>
                      {messages.assignMe}
                    </button>
                  </p>
                )}
              </div>
              {canChange && !confirming && (
                <p>
                  <button type="button" onClick={() => { setEditing(true); setNotice(null); setFailure(null); }}>
                    {messages.editTask}
                  </button>{' '}
                  <button type="button" onClick={() => { setConfirming(true); setNotice(null); setFailure(null); }}>
                    {messages.deleteTask}
                  </button>{' '}
                  <button type="button" onClick={() => select(null)}>
                    {messages.closeTask}
                  </button>
                </p>
              )}
              {!canChange && (
                <p>
                  <button type="button" onClick={() => select(null)}>
                    {messages.closeTask}
                  </button>
                </p>
              )}
            </>
          )}
          {confirming && (
            <div role="alertdialog" aria-labelledby="confirm-delete-task-text" aria-modal="false">
              <p id="confirm-delete-task-text">{messages.confirmDeleteTask(selected.title)}</p>
              <button type="button" disabled={busy} onClick={() => confirmDelete(selected)}>
                {messages.confirmDeleteButton}
              </button>{' '}
              <button type="button" ref={cancelButton} disabled={busy} onClick={() => setConfirming(false)}>
                {messages.cancel}
              </button>
            </div>
          )}
        </section>
      )}
    </section>
  );
}
