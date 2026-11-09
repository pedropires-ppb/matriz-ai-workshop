import { useId, useState } from 'react';
import { Button, ConfirmBox, Link, LoadFailed, Loading, NotAccessible, Notice, ProgressPanel, StateBadge } from './components';
import { listProjectMembers } from './membersApi';
import MembersSection from './MembersSection';
import { messages } from './messages';
import { type Flash, go, useFlash } from './navigation';
import ProjectForm from './ProjectForm';
import { completeProject, deleteProject, getProject, updateProject } from './projectsApi';
import { ALL_PROJECTS, type Route } from './router';
import { useApi, useLoad, useSubmit } from './session';
import TasksSection from './TasksSection';
import { refusal } from './validation';

type ProjectRoute = Extract<Route, { screen: 'project' }>;

/**
 * Project detail (US-006, US-007, US-008, US-010, US-016, US-026): the project's data, its progress, its
 * renaming, its completion and its deletion after a confirmation, its members, and its tasks. Edit, Complete,
 * and Delete are here only (I13 decision 2; I8 decision 5). After a task is created here, the project is
 * fetched again, so the progress is the API's (I5 decision 3). When completing is refused, the project and
 * its tasks are fetched again, and the message says whether it is still Active (I8 decision 6).
 */
export default function ProjectScreen({ route, notice }: { route: ProjectRoute; notice: Flash | null }) {
  const { projectId } = route;
  const { run } = useApi();
  const [shown, setShown] = useFlash(notice);
  const [failure, setFailure] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [confirming, setConfirming] = useState<'delete' | 'complete' | null>(null);
  const [taskListVersion, setTaskListVersion] = useState(0);
  const deletion = useSubmit();
  const completion = useSubmit();
  const unavailableId = useId();
  const project = useLoad((session) => getProject(session, projectId), [projectId]);
  const members = useLoad((session) => listProjectMembers(session, projectId), [projectId]);

  function clearNotices() {
    setShown(null);
    setFailure(null);
  }

  function confirmDelete() {
    void deletion.submit(async () => {
      const result = await run((session) => deleteProject(session, projectId));
      if (result.outcome === 'ok') {
        go(ALL_PROJECTS, { replace: true, notice: messages.deleted });
        return;
      }
      setConfirming(null);
      setFailure(refusal(result, 'project'));
    });
  }

  function confirmComplete() {
    void completion.submit(async () => {
      const result = await run((session) => completeProject(session, projectId));
      setConfirming(null);
      if (result.outcome === 'ok') {
        project.set(result.value);
        setShown(messages.projectCompletedNotice);
        return;
      }
      if (result.outcome !== 'completed') {
        setFailure(refusal(result, 'project'));
        return;
      }
      const fresh = await run((session) => getProject(session, projectId));
      setTaskListVersion((version) => version + 1);
      if (fresh.outcome !== 'ok') {
        setFailure(refusal(fresh, 'project'));
        return;
      }
      project.set(fresh.value);
      setFailure(fresh.value.state === 'ACTIVE' ? messages.projectHasUnfinishedTasks : messages.projectCompleted);
    });
  }

  if (project.state.status === 'loading') {
    return <Loading />;
  }
  if (project.state.status === 'failed') {
    const result = project.state.result;
    return result.outcome === 'notFound' ? (
      <NotAccessible text={messages.projectNotFound} />
    ) : (
      <LoadFailed text={refusal(result, 'project')} onRetry={project.retry} />
    );
  }
  const current = project.state.value;
  const isOwner = current.myRole === 'PROJECT_OWNER';
  const canChange = isOwner && current.state === 'ACTIVE';
  const { toDo, inProgress } = current.progress.taskCounts;
  const hasUnfinishedTasks = toDo + inProgress > 0;

  return (
    <section aria-labelledby="project-heading" className="screen">
      <p className="back">
        <Link to={ALL_PROJECTS}>{messages.backToProjects}</Link>
      </p>
      <h2 id="project-heading">{current.name}</h2>
      {shown !== null && <Notice kind="success">{shown}</Notice>}
      {failure !== null && <Notice kind="error">{failure}</Notice>}
      <div className="project-layout">
        <section aria-labelledby="project-details-heading" className="panel project-details">
          <h3 id="project-details-heading">{messages.details}</h3>
          {editing ? (
            <ProjectForm
              idPrefix="edit-project"
              initial={{ name: current.name, description: current.description ?? '' }}
              submitLabel={messages.saveChanges}
              onSubmit={(content) => run((session) => updateProject(session, projectId, content))}
              onSaved={(saved) => {
                project.set(saved);
                setEditing(false);
                setFailure(null);
                setShown(messages.saved);
              }}
              onCancel={() => setEditing(false)}
            />
          ) : (
            <>
              <p className="description">{current.description ?? messages.noDescription}</p>
              <dl>
                <dt>{messages.stateLabel}</dt>
                <dd>
                  <StateBadge kind="project" value={current.state} />
                </dd>
                <dt>{messages.roleLabel}</dt>
                <dd>
                  <StateBadge kind="role" value={current.myRole} />
                </dd>
              </dl>
              {canChange && confirming === null && (
                <div className="actions">
                  <Button
                    onClick={() => {
                      setEditing(true);
                      clearNotices();
                    }}
                  >
                    {messages.editProject}
                  </Button>
                  <Button
                    disabled={hasUnfinishedTasks}
                    describedBy={hasUnfinishedTasks ? unavailableId : undefined}
                    onClick={() => {
                      setConfirming('complete');
                      clearNotices();
                    }}
                  >
                    {messages.completeProject}
                  </Button>
                  <Button
                    variant="danger"
                    onClick={() => {
                      setConfirming('delete');
                      clearNotices();
                    }}
                  >
                    {messages.deleteProject}
                  </Button>
                </div>
              )}
              {canChange && confirming === null && hasUnfinishedTasks && (
                <p id={unavailableId} className="hint">
                  {messages.completeUnavailable}
                </p>
              )}
            </>
          )}
          {confirming === 'delete' && (
            <ConfirmBox
              text={messages.confirmDelete(current.name)}
              confirmLabel={messages.confirmDeleteButton}
              busy={deletion.busy}
              onConfirm={confirmDelete}
              onCancel={() => setConfirming(null)}
            />
          )}
          {confirming === 'complete' && (
            <ConfirmBox
              text={messages.confirmComplete(current.name)}
              confirmLabel={messages.confirmCompleteButton}
              busy={completion.busy}
              onConfirm={confirmComplete}
              onCancel={() => setConfirming(null)}
            />
          )}
        </section>
        <ProgressPanel progress={current.progress} />
        <MembersSection loaded={members.state} onRetry={members.retry} />
        <TasksSection
          route={route}
          project={current}
          onTasksChanged={project.reload}
          listVersion={taskListVersion}
        />
      </div>
    </section>
  );
}
