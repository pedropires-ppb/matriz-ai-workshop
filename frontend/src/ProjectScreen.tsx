import { useState } from 'react';
import { Button, ConfirmBox, Link, LoadFailed, Loading, Notice, StateBadge } from './components';
import { listProjectMembers } from './membersApi';
import MembersSection from './MembersSection';
import { messages } from './messages';
import { type Flash, go, useFlash } from './navigation';
import ProjectForm from './ProjectForm';
import { deleteProject, getProject, updateProject } from './projectsApi';
import { ALL_PROJECTS, HOME, type Route } from './router';
import { useApi, useLoad, useSubmit } from './session';
import TasksSection from './TasksSection';
import { refusal } from './validation';

type ProjectRoute = Extract<Route, { screen: 'project' }>;

/** A project that cannot be shown: it does not exist, or the person is not a member (I13 decision 4). */
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

/**
 * Project detail (US-007, US-008, US-016, US-026): the project's data, its renaming and its deletion after
 * a confirmation, its members, and its tasks. Edit and Delete are here only (I13 decision 2).
 */
export default function ProjectScreen({ route, notice }: { route: ProjectRoute; notice: Flash | null }) {
  const { projectId } = route;
  const { run } = useApi();
  const [shown, setShown] = useFlash(notice);
  const [failure, setFailure] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const deletion = useSubmit();
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
      setConfirming(false);
      setFailure(refusal(result, 'project'));
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
              {canChange && !confirming && (
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
                    variant="danger"
                    onClick={() => {
                      setConfirming(true);
                      clearNotices();
                    }}
                  >
                    {messages.deleteProject}
                  </Button>
                </div>
              )}
            </>
          )}
          {confirming && (
            <ConfirmBox
              text={messages.confirmDelete(current.name)}
              confirmLabel={messages.confirmDeleteButton}
              busy={deletion.busy}
              onConfirm={confirmDelete}
              onCancel={() => setConfirming(false)}
            />
          )}
        </section>
        <MembersSection loaded={members.state} onRetry={members.retry} />
        <TasksSection route={route} project={current} />
      </div>
    </section>
  );
}
