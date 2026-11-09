import { LoadFailed, Loading } from './components';
import type { Member } from './membersApi';
import { messages } from './messages';
import type { Loaded } from './session';
import { refusal } from './validation';

interface Props {
  loaded: Loaded<Member[]>;
  onRetry: () => void;
}

/** The members of a project and their roles, which every member may see (US-026). */
export default function MembersSection({ loaded, onRetry }: Props) {
  return (
    <section aria-labelledby="members-heading" className="panel project-members">
      <h3 id="members-heading">{messages.members}</h3>
      {loaded.status === 'loading' && <Loading />}
      {loaded.status === 'failed' && <LoadFailed text={refusal(loaded.result, 'project')} onRetry={onRetry} />}
      {loaded.status === 'ready' && (
        <ul aria-label={messages.members} className="rows">
          {loaded.value.map((member) => (
            <li key={member.person.personId}>
              {member.person.displayName} · {messages.roles[member.role]}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
