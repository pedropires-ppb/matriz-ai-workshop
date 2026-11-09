import { type FormEvent, useId, useState } from 'react';
import { Button, ConfirmBox, Field, LoadFailed, Loading, Notice } from './components';
import type { PersonSummary, Result } from './http';
import { addParticipant, listEligibleParticipants, type Member, removeParticipant } from './membersApi';
import { messages } from './messages';
import type { Project } from './projectsApi';
import { type Loaded, useApi, useSubmit } from './session';
import { refusal } from './validation';

interface Props {
  project: Project;
  loaded: Loaded<Member[]>;
  onRetry: () => void;
  /** Fetches the member list again. */
  onMembersChanged: () => void;
  /** Fetches the project again, after a change was refused because the project may have changed. */
  onProjectStale: () => Promise<Result<Project>>;
}

/**
 * The members of a project and their roles, which every member may see (US-026). The Project Owner of an
 * Active project adds Project Participants found by name, and removes them after a confirmation (US-002,
 * US-009; I6 decisions 3 and 5). After each change the member list is fetched again (I6 decision 6); after a
 * `409`, the project is fetched too, and the message says why (I6 decision 4).
 */
export default function MembersSection({ project, loaded, onRetry, onMembersChanged, onProjectStale }: Props) {
  const { run } = useApi();
  const { busy, submit } = useSubmit();
  const [shown, setShown] = useState<string | null>(null);
  const [failure, setFailure] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);
  const [name, setName] = useState('');
  const [found, setFound] = useState<PersonSummary[] | null>(null);
  const [searchFailure, setSearchFailure] = useState<string | null>(null);
  const [removing, setRemoving] = useState<PersonSummary | null>(null);
  const hintId = useId();
  const projectId = project.projectId;
  const canManage = project.myRole === 'PROJECT_OWNER' && project.state === 'ACTIVE';

  function clearNotices() {
    setShown(null);
    setFailure(null);
  }

  /** The message for a `409`, once the project has been fetched again: Completed, or `otherwise`. */
  async function conflictText(otherwise: string): Promise<string> {
    const fresh = await onProjectStale();
    if (fresh.outcome !== 'ok') {
      return refusal(fresh, 'project');
    }
    return fresh.value.state === 'COMPLETED' ? messages.projectCompleted : otherwise;
  }

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void submit(async () => {
      clearNotices();
      setSearchFailure(null);
      const result = await run((session) => listEligibleParticipants(session, projectId, name));
      if (result.outcome === 'ok') {
        setFound(result.value);
      } else {
        setFound(null);
        setSearchFailure(refusal(result, 'project'));
      }
    });
  }

  function closeAdding() {
    setAdding(false);
    setName('');
    setFound(null);
    setSearchFailure(null);
  }

  function add(person: PersonSummary) {
    void submit(async () => {
      clearNotices();
      const result = await run((session) => addParticipant(session, projectId, person.personId));
      if (result.outcome === 'ok' || result.outcome === 'completed') {
        setFound((earlier) => earlier?.filter((other) => other.personId !== person.personId) ?? null);
        onMembersChanged();
      }
      if (result.outcome === 'ok') {
        setShown(messages.memberAdded(result.value.person.displayName));
      } else if (result.outcome === 'completed') {
        setFailure(await conflictText(messages.alreadyMember(person.displayName)));
      } else {
        setFailure(refusal(result, 'project', { invalid: messages.personNotFound }));
      }
    });
  }

  function confirmRemove() {
    const person = removing;
    if (person === null) {
      return;
    }
    void submit(async () => {
      clearNotices();
      const result = await run((session) => removeParticipant(session, projectId, person.personId));
      setRemoving(null);
      onMembersChanged();
      if (result.outcome === 'ok') {
        setShown(messages.memberRemoved(person.displayName));
      } else if (result.outcome === 'completed') {
        setFailure(await conflictText(messages.memberHasAssignedTasks(person.displayName)));
      } else {
        setFailure(refusal(result, 'project'));
      }
    });
  }

  return (
    <section aria-labelledby="members-heading" className="panel project-members">
      <h3 id="members-heading">{messages.members}</h3>
      {shown !== null && <Notice kind="success">{shown}</Notice>}
      {failure !== null && <Notice kind="error">{failure}</Notice>}
      {loaded.status === 'loading' && <Loading />}
      {loaded.status === 'failed' && <LoadFailed text={refusal(loaded.result, 'project')} onRetry={onRetry} />}
      {loaded.status === 'ready' && (
        <ul aria-label={messages.members} className="rows">
          {loaded.value.map((member) => {
            const removable = canManage && member.role === 'PROJECT_PARTICIPANT';
            const reasonId = `${hintId}-${member.person.personId}`;
            return (
              <li key={member.person.personId}>
                {member.person.displayName} · {messages.roles[member.role]}
                {removable && removing === null && (
                  <Button
                    busy={busy}
                    disabled={member.hasAssignedTasks}
                    describedBy={member.hasAssignedTasks ? reasonId : undefined}
                    onClick={() => {
                      clearNotices();
                      setRemoving(member.person);
                    }}
                  >
                    {messages.removeMember}
                  </Button>
                )}
                {removable && member.hasAssignedTasks && (
                  <p id={reasonId} className="hint">
                    {messages.removeUnavailable}
                  </p>
                )}
              </li>
            );
          })}
        </ul>
      )}
      {canManage && removing !== null && (
        <ConfirmBox
          text={messages.confirmRemoveMember(removing.displayName, project.name)}
          confirmLabel={messages.removeMember}
          busy={busy}
          onConfirm={confirmRemove}
          onCancel={() => setRemoving(null)}
        />
      )}
      {canManage && !adding && (
        <Button
          onClick={() => {
            clearNotices();
            setAdding(true);
          }}
        >
          {messages.addParticipant}
        </Button>
      )}
      {canManage && adding && (
        <div role="group" aria-label={messages.addParticipant}>
          <form role="search" onSubmit={search} className="controls">
            <Field id="participant-search" label={messages.personName} type="search" value={name} onChange={setName} autoFocus />
            <Button type="submit" busy={busy}>
              {messages.search}
            </Button>
            <Button onClick={closeAdding} disabled={busy}>
              {messages.cancel}
            </Button>
          </form>
          {searchFailure !== null && <Notice kind="error">{searchFailure}</Notice>}
          {found !== null &&
            (found.length === 0 ? (
              <p className="secondary-text">{messages.noPeopleToAdd}</p>
            ) : (
              <ul aria-label={messages.peopleToAdd} className="rows">
                {found.map((person) => (
                  <li key={person.personId}>
                    {person.displayName}
                    <Button busy={busy} onClick={() => add(person)}>
                      {messages.add}
                    </Button>
                  </li>
                ))}
              </ul>
            ))}
        </div>
      )}
    </section>
  );
}
