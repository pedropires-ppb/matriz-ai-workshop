import { useState } from 'react';
import CommentsSection from './CommentsSection';
import { Button, ConfirmBox, Link, LoadFailed, Loading, Notice, SelectField, StateBadge } from './components';
import type { Refused, Result } from './http';
import { listProjectMembers } from './membersApi';
import { messages } from './messages';
import { type Flash, go, useFlash } from './navigation';
import { getProject, type Project } from './projectsApi';
import { NotAccessible } from './ProjectScreen';
import type { Route } from './router';
import { useApi, useLoad, useSubmit } from './session';
import TaskForm from './TaskForm';
import {
  assignTask,
  changeTaskState,
  deleteTask,
  getTask,
  TASK_PRIORITIES,
  TASK_STATES,
  type Task,
  type TaskPriority,
  type TaskState,
  unassignTask,
  updateTask,
} from './tasksApi';
import { refusal } from './validation';

type TaskRoute = Extract<Route, { screen: 'task' }>;

/**
 * Task detail (US-005, US-011 to US-013, US-017, US-018, US-020 to US-022; I13 decision 3): the task's data,
 * its editing, state, priority, assignees, comments, and deletion. Each change shows the task that the API
 * confirmed. "Back to the project" returns to the project's filters and page.
 */
export default function TaskScreen({ route, notice }: { route: TaskRoute; notice: Flash | null }) {
  const { projectId, taskId, list } = route;
  const project = useLoad((session) => getProject(session, projectId), [projectId]);
  const task = useLoad((session) => getTask(session, projectId, taskId), [projectId, taskId]);
  const backTo: Route = { screen: 'project', projectId, list };

  if (project.state.status === 'loading' || task.state.status === 'loading') {
    return <Loading />;
  }
  for (const loaded of [project, task]) {
    if (loaded.state.status === 'failed') {
      const result = loaded.state.result;
      if (result.outcome === 'notFound') {
        return <NotAccessible text={loaded === project ? messages.projectNotFound : messages.taskNotFound} />;
      }
      return <LoadFailed text={refusal(result, 'task')} onRetry={loaded.retry} />;
    }
  }
  if (project.state.status !== 'ready' || task.state.status !== 'ready') {
    return null;
  }
  return (
    <TaskDetail
      project={project.state.value}
      task={task.state.value}
      backTo={backTo}
      notice={notice}
      onConfirmed={task.set}
      onRefused={() => {
        project.reload();
        task.reload();
      }}
    />
  );
}

interface DetailProps {
  project: Project;
  task: Task;
  backTo: Route;
  notice: Flash | null;
  /** Shows the task that the API confirmed. */
  onConfirmed: (task: Task) => void;
  /** Loads the project and the task again after a refusal, so the screen shows what the server holds. */
  onRefused: () => void;
}

function TaskDetail({ project, task, backTo, notice, onConfirmed, onRefused }: DetailProps) {
  const { run, session } = useApi();
  const [shown, setShown] = useFlash(notice);
  const [failure, setFailure] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [personToAssign, setPersonToAssign] = useState('');
  const { busy, submit } = useSubmit();
  const projectId = project.projectId;
  const taskId = task.taskId;
  const isOwner = project.myRole === 'PROJECT_OWNER';
  const active = project.state === 'ACTIVE';
  const canChange = isOwner && active;
  const me = session.person.personId;
  const assignedToMe = task.assignees.some((person) => person.personId === me);
  const canChangeState = active && (isOwner || assignedToMe);
  const members = useLoad((current) => listProjectMembers(current, projectId), [projectId]);
  const candidates =
    members.state.status === 'ready'
      ? members.state.value.filter((member) => !task.assignees.some((person) => person.personId === member.person.personId))
      : [];

  function clearNotices() {
    setShown(null);
    setFailure(null);
  }

  /** Sends one change at a time; the screen then shows the confirmed task, or the refusal. */
  function change(
    request: () => Promise<Result<Task>>,
    confirmed: (changed: Task) => string,
    texts: Partial<Record<Refused['outcome'], string>> = {},
  ) {
    void submit(async () => {
      clearNotices();
      const result = await request();
      if (result.outcome === 'ok') {
        onConfirmed(result.value);
        setShown(confirmed(result.value));
      } else {
        setFailure(refusal(result, 'task', texts));
        onRefused();
      }
    });
  }

  function changeState(state: TaskState) {
    change(
      () => run((current) => changeTaskState(current, projectId, taskId, state)),
      (changed) => messages.stateChanged(messages.taskStates[changed.state]),
      { forbidden: messages.stateForbidden },
    );
  }

  function changePriority(priority: TaskPriority) {
    change(
      () => run((current) => updateTask(current, projectId, taskId, { priority })),
      (changed) => messages.priorityChanged(messages.taskPriorities[changed.priority]),
    );
  }

  function assign(personId: string) {
    change(
      () => run((current) => assignTask(current, projectId, taskId, personId)),
      () => {
        setPersonToAssign('');
        return messages.assigneeAdded;
      },
      { forbidden: messages.assignmentForbidden, invalid: messages.notMemberOfProject },
    );
  }

  function unassign(personId: string) {
    change(
      () => run((current) => unassignTask(current, projectId, taskId, personId)),
      () => messages.assigneeRemoved,
      { forbidden: messages.assignmentForbidden },
    );
  }

  function confirmDelete() {
    void submit(async () => {
      clearNotices();
      const result = await run((current) => deleteTask(current, projectId, taskId));
      if (result.outcome === 'ok') {
        go(backTo, { replace: true, notice: messages.taskDeleted });
        return;
      }
      setConfirming(false);
      setFailure(refusal(result, 'task'));
      onRefused();
    });
  }

  const readOnlyText = !active ? messages.tasksReadOnlyCompleted : isOwner ? null : messages.tasksReadOnlyParticipant;

  return (
    <section aria-labelledby="task-heading" className="screen">
      <p className="back">
        <Link to={backTo}>{messages.backToProject}</Link> · <span className="secondary-text">{project.name}</span>
      </p>
      <h2 id="task-heading">{task.title}</h2>
      {shown !== null && <Notice kind="success">{shown}</Notice>}
      {failure !== null && <Notice kind="error">{failure}</Notice>}
      <div className="task-layout">
        <section aria-labelledby="task-details-heading" className="panel">
          <h3 id="task-details-heading">{messages.details}</h3>
          {readOnlyText !== null && <p className="secondary-text">{readOnlyText}</p>}
          {editing ? (
            <TaskForm
              idPrefix="edit-task"
              initial={{ title: task.title, description: task.description ?? '', priority: task.priority }}
              withPriority={false}
              submitLabel={messages.saveChanges}
              onSubmit={(content) =>
                run((current) =>
                  updateTask(current, projectId, taskId, { title: content.title, description: content.description }),
                )
              }
              onSaved={(saved) => {
                onConfirmed(saved);
                setEditing(false);
                setFailure(null);
                setShown(messages.taskSaved);
              }}
              onCancel={() => setEditing(false)}
            />
          ) : (
            <p className="description">{task.description ?? messages.noDescription}</p>
          )}
          {canChangeState || canChange ? (
            <div className="controls">
              <SelectField
                id="task-state"
                label={messages.stateLabel}
                value={task.state}
                disabled={busy || !canChangeState}
                options={TASK_STATES.map((value) => ({ value, label: messages.taskStates[value] }))}
                onChange={changeState}
              />
              <SelectField
                id="task-priority"
                label={messages.priorityLabel}
                value={task.priority}
                disabled={busy || !canChange}
                options={TASK_PRIORITIES.map((value) => ({ value, label: messages.taskPriorities[value] }))}
                onChange={changePriority}
              />
            </div>
          ) : (
            <dl>
              <dt>{messages.stateLabel}</dt>
              <dd>
                <StateBadge kind="task" value={task.state} />
              </dd>
              <dt>{messages.priorityLabel}</dt>
              <dd>
                <StateBadge kind="priority" value={task.priority} />
              </dd>
            </dl>
          )}
          {canChange && !editing && !confirming && (
            <div className="actions">
              <Button
                onClick={() => {
                  setEditing(true);
                  clearNotices();
                }}
              >
                {messages.editTask}
              </Button>
              <Button
                variant="danger"
                onClick={() => {
                  setConfirming(true);
                  clearNotices();
                }}
              >
                {messages.deleteTask}
              </Button>
            </div>
          )}
          {confirming && (
            <ConfirmBox
              text={messages.confirmDeleteTask(task.title)}
              confirmLabel={messages.confirmDeleteButton}
              busy={busy}
              onConfirm={confirmDelete}
              onCancel={() => setConfirming(false)}
            />
          )}
        </section>
        <section aria-labelledby="assignees-heading" className="panel">
          <h3 id="assignees-heading">{messages.assigneesLabel}</h3>
          <div role="group" aria-label={messages.assigneesLabel}>
            {task.assignees.length === 0 ? (
              <p>{messages.noAssignees}</p>
            ) : (
              <ul aria-label={messages.assigneesLabel} className="rows">
                {task.assignees.map((person) => (
                  <li key={person.personId}>
                    {person.displayName}
                    {active && (isOwner || person.personId === me) && (
                      <Button busy={busy} onClick={() => unassign(person.personId)}>
                        {isOwner ? messages.unassign(person.displayName) : messages.unassignMe}
                      </Button>
                    )}
                  </li>
                ))}
              </ul>
            )}
            {active && isOwner && members.state.status === 'loading' && <Loading />}
            {active && isOwner && members.state.status === 'failed' && (
              <LoadFailed text={refusal(members.state.result, 'project')} onRetry={members.retry} />
            )}
            {active &&
              isOwner &&
              members.state.status === 'ready' &&
              (candidates.length === 0 ? (
                <p className="secondary-text">{messages.allMembersAssigned}</p>
              ) : (
                <div className="controls">
                  <SelectField
                    id="task-assignee"
                    label={messages.personToAssign}
                    value={personToAssign}
                    disabled={busy}
                    options={[
                      { value: '', label: messages.chooseMember },
                      ...candidates.map((member) => ({ value: member.person.personId, label: member.person.displayName })),
                    ]}
                    onChange={setPersonToAssign}
                  />
                  <Button busy={busy} disabled={personToAssign === ''} onClick={() => assign(personToAssign)}>
                    {messages.assign}
                  </Button>
                </div>
              ))}
            {active && !isOwner && !assignedToMe && (
              <p>
                <Button busy={busy} onClick={() => assign(me)}>
                  {messages.assignMe}
                </Button>
              </p>
            )}
          </div>
        </section>
        <CommentsSection projectId={projectId} taskId={taskId} />
      </div>
    </section>
  );
}
