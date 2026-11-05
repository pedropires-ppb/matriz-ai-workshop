import { useCallback, useEffect, useRef, useState } from 'react';
import { type Session, SessionEndedError } from './api';
import { messages } from './messages';
import ProjectForm from './ProjectForm';
import { listProjectMembers, type Member } from './membersApi';
import MembersSection from './MembersSection';
import TasksSection from './TasksSection';
import { refusal } from './projectValidation';
import { deleteProject, getProject, type Project, type ProjectResult, updateProject } from './projectsApi';

interface Props {
  session: Session;
  projectId: string;
  onBack: () => void;
  onSessionEnded: () => void;
}

type Load = { status: 'loading' } | { status: 'ready'; project: Project } | { status: 'refused'; text: string };

/** One project (US-007, US-008, US-016): its details, its renaming, and its deletion after a confirmation. */
export default function ProjectScreen({ session, projectId, onBack, onSessionEnded }: Props) {
  const [load, setLoad] = useState<Load>({ status: 'loading' });
  const [editing, setEditing] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [failure, setFailure] = useState<string | null>(null);
  const [members, setMembers] = useState<Member[] | null>(null);
  const [membersFailed, setMembersFailed] = useState(false);
  const cancelButton = useRef<HTMLButtonElement>(null);

  /** Runs a request; a 401 sends the person to sign in. */
  const guard = useCallback(
    async <T,>(request: () => Promise<ProjectResult<T>>): Promise<ProjectResult<T>> => {
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
    void guard(() => getProject(session, projectId)).then((result) => {
      if (current) {
        setLoad(
          result.outcome === 'ok'
            ? { status: 'ready', project: result.value }
            : { status: 'refused', text: refusal(result) ?? messages.somethingFailed },
        );
      }
    });
    return () => {
      current = false;
    };
  }, [guard, session, projectId]);

  useEffect(() => {
    let current = true;
    void guard(() => listProjectMembers(session, projectId)).then((result) => {
      if (current) {
        setMembers(result.outcome === 'ok' ? result.value : null);
        setMembersFailed(result.outcome !== 'ok');
      }
    });
    return () => {
      current = false;
    };
  }, [guard, session, projectId]);

  useEffect(() => {
    if (confirming) {
      cancelButton.current?.focus();
    }
  }, [confirming]);

  async function confirmDelete() {
    setDeleting(true);
    setFailure(null);
    const result = await guard(() => deleteProject(session, projectId));
    setDeleting(false);
    if (result.outcome === 'ok') {
      onBack();
      return;
    }
    setConfirming(false);
    setFailure(refusal(result));
  }

  if (load.status === 'loading') {
    return <p>{messages.loading}</p>;
  }
  if (load.status === 'refused') {
    return (
      <section>
        <p role="alert">{load.text}</p>
        <button type="button" onClick={onBack}>
          {messages.backToProjects}
        </button>
      </section>
    );
  }
  const project = load.project;
  const isOwner = project.myRole === 'PROJECT_OWNER';
  const canChange = isOwner && project.state === 'ACTIVE';

  return (
    <section aria-labelledby="project-heading">
      <p>
        <button type="button" onClick={onBack}>
          {messages.backToProjects}
        </button>
      </p>
      <h2 id="project-heading">{project.name}</h2>
      {notice !== null && <p role="status">{notice}</p>}
      {failure !== null && <p role="alert">{failure}</p>}
      {editing ? (
        <ProjectForm
          idPrefix="edit-project"
          initialName={project.name}
          initialDescription={project.description ?? ''}
          submitLabel={messages.saveChanges}
          onSubmit={(content) => guard(() => updateProject(session, projectId, content))}
          onSaved={(saved) => {
            setLoad({ status: 'ready', project: saved });
            setEditing(false);
            setFailure(null);
            setNotice(messages.saved);
          }}
          onCancel={() => setEditing(false)}
        />
      ) : (
        <>
          <p>{project.description ?? messages.noDescription}</p>
          <dl>
            <dt>{messages.stateLabel}</dt>
            <dd>{messages.states[project.state]}</dd>
            <dt>{messages.roleLabel}</dt>
            <dd>{messages.roles[project.myRole]}</dd>
          </dl>
          {canChange && !confirming && (
            <p>
              <button type="button" onClick={() => { setEditing(true); setNotice(null); setFailure(null); }}>
                {messages.editProject}
              </button>{' '}
              <button type="button" onClick={() => { setConfirming(true); setNotice(null); setFailure(null); }}>
                {messages.deleteProject}
              </button>
            </p>
          )}
        </>
      )}
      {confirming && (
        <div role="alertdialog" aria-labelledby="confirm-delete-text" aria-modal="false">
          <p id="confirm-delete-text">{messages.confirmDelete(project.name)}</p>
          <button type="button" disabled={deleting} onClick={() => void confirmDelete()}>
            {messages.confirmDeleteButton}
          </button>{' '}
          <button type="button" ref={cancelButton} disabled={deleting} onClick={() => setConfirming(false)}>
            {messages.cancel}
          </button>
        </div>
      )}
      <MembersSection members={members} failed={membersFailed} />
      <TasksSection
        session={session}
        projectId={projectId}
        canChange={canChange}
        readOnlyText={
          project.state === 'COMPLETED'
            ? messages.tasksReadOnlyCompleted
            : isOwner
              ? null
              : messages.tasksReadOnlyParticipant
        }
        onSessionEnded={onSessionEnded}
      />
    </section>
  );
}
