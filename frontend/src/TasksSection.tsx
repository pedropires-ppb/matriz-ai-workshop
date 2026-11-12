import { useEffect, useState } from 'react';
import { Button, EmptyState, Link, LoadFailed, Loading, Notice, Pagination, StateBadge } from './components';
import { messages } from './messages';
import { go } from './navigation';
import type { Project } from './projectsApi';
import type { Route, TaskListQuery } from './router';
import { useApi, useLoad } from './session';
import TaskForm from './TaskForm';
import { createTask, listTasks, TASK_PRIORITIES, TASK_STATES, TASKS_PAGE_SIZE } from './tasksApi';
import { refusal } from './validation';

type ProjectRoute = Extract<Route, { screen: 'project' }>;

function toggled<T>(values: readonly T[], value: T): T[] {
  return values.includes(value) ? values.filter((one) => one !== value) : [...values, value];
}

interface Props {
  route: ProjectRoute;
  project: Project;
  /** Called after a task is created, so that the project's progress is fetched again. */
  onTasksChanged: () => void;
  /** A change of this number fetches the list again, for example after a refused completion (I8 decision 6). */
  listVersion: number;
}

/**
 * The tasks of a project (US-004, US-009 to US-013, US-017): the list with its filters and pages, each task
 * with its state, priority, and assignees, and the creation. The filters and the page are in the address,
 * so the task detail returns to them (I13 decision 3). After a creation the page is fetched again, so the
 * list and the count are the server's (I13 scope decision 10).
 */
export default function TasksSection({ route, project, onTasksChanged, listVersion }: Props) {
  const { run } = useApi();
  const { projectId, list } = route;
  const [creating, setCreating] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const tasks = useLoad(
    (session) => listTasks(session, projectId, { ...list, size: TASKS_PAGE_SIZE }),
    [projectId, list.states.join(), list.priorities.join(), list.page, listVersion],
  );
  const isOwner = project.myRole === 'PROJECT_OWNER';
  const canChange = isOwner && project.state === 'ACTIVE';
  const filtered = list.states.length > 0 || list.priorities.length > 0;
  const readOnlyText =
    project.state === 'COMPLETED' ? messages.tasksReadOnlyCompleted : isOwner ? null : messages.tasksReadOnlyParticipant;

  function show(next: TaskListQuery) {
    setNotice(null);
    go({ ...route, list: next }, { replace: true });
  }

  // A page past the last one, for example after the last task of a page was deleted, shows the last page.
  useEffect(() => {
    if (tasks.state.status === 'ready' && tasks.state.value.items.length === 0 && list.page > 1) {
      go({ ...route, list: { ...list, page: Math.max(1, tasks.state.value.totalPages) } }, { replace: true });
    }
  }, [tasks.state, list, route]);

  return (
    <section aria-labelledby="tasks-heading" className="panel project-tasks">
      <h3 id="tasks-heading">{messages.tasks}</h3>
      <div className="filters">
        <div role="group" aria-label={messages.filterByState}>
          <span>{messages.filterByState}:</span>
          {TASK_STATES.map((state) => (
            <label key={state} className="check">
              <input
                type="checkbox"
                checked={list.states.includes(state)}
                onChange={() => show({ ...list, states: toggled(list.states, state), page: 1 })}
              />
              {messages.taskStates[state]}
            </label>
          ))}
        </div>
        <div role="group" aria-label={messages.filterByPriority}>
          <span>{messages.filterByPriority}:</span>
          {TASK_PRIORITIES.map((priority) => (
            <label key={priority} className="check">
              <input
                type="checkbox"
                checked={list.priorities.includes(priority)}
                onChange={() => show({ ...list, priorities: toggled(list.priorities, priority), page: 1 })}
              />
              {messages.taskPriorities[priority]}
            </label>
          ))}
        </div>
        {filtered && <Button onClick={() => show({ states: [], priorities: [], page: 1 })}>{messages.clearFilters}</Button>}
      </div>
      {readOnlyText !== null && <p className="secondary-text">{readOnlyText}</p>}
      {canChange && !creating && (
        <p>
          <Button
            variant="primary"
            onClick={() => {
              setNotice(null);
              setCreating(true);
            }}
          >
            {messages.newTask}
          </Button>
        </p>
      )}
      {creating && (
        <TaskForm
          idPrefix="new-task"
          initial={{ title: '', description: '', priority: 'MEDIUM' }}
          withPriority
          submitLabel={messages.createTask}
          onSubmit={(content) => run((session) => createTask(session, projectId, content))}
          onSaved={() => {
            setCreating(false);
            setNotice(messages.taskCreated);
            onTasksChanged();
            const total = tasks.state.status === 'ready' ? tasks.state.value.totalItems : 0;
            const last = Math.max(1, Math.ceil((total + 1) / TASKS_PAGE_SIZE));
            if (last === list.page) {
              tasks.reload();
            } else {
              go({ ...route, list: { ...list, page: last } }, { replace: true });
            }
          }}
          onCancel={() => setCreating(false)}
        />
      )}
      {notice !== null && <Notice kind="success">{notice}</Notice>}
      {tasks.state.status === 'loading' && <Loading />}
      {tasks.state.status === 'failed' && <LoadFailed text={refusal(tasks.state.result, 'task')} onRetry={tasks.retry} />}
      {tasks.state.status === 'ready' && (
        <>
          <p id="task-count" className="count" aria-live="polite">
            {messages.taskCount(tasks.state.value.totalItems)}
          </p>
          {tasks.state.value.totalItems === 0 ? (
            <EmptyState text={filtered ? messages.noTaskMatches : messages.noTasks} />
          ) : (
            <ul aria-label={messages.tasks} className="rows">
              {tasks.state.value.items.map((task) => (
                <li key={task.taskId}>
                  <Link to={{ screen: 'task', projectId, taskId: task.taskId, list }}>{task.title}</Link>
                  <span className="badges">
                    <StateBadge kind="task" value={task.state} />
                    <StateBadge kind="priority" value={task.priority} />
                  </span>
                  <p className="secondary-text assignees">
                    {task.assignees.length === 0
                      ? messages.noAssignees
                      : `${messages.assigneesLabel}: ${task.assignees.map((person) => person.displayName).join(', ')}`}
                  </p>
                </li>
              ))}
            </ul>
          )}
          <Pagination
            page={tasks.state.value.page}
            totalPages={tasks.state.value.totalPages}
            onPage={(page) => {
              setNotice(null);
              go({ ...route, list: { ...list, page } });
            }}
          />
        </>
      )}
    </section>
  );
}
