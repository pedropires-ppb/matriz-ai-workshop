import type { Member } from './membersApi';
import { messages } from './messages';

interface Props {
  /** `null` while the list is being fetched. */
  members: Member[] | null;
  failed: boolean;
}

/** The members of a project and their roles, which every member may see (US-026). */
export default function MembersSection({ members, failed }: Props) {
  return (
    <section aria-labelledby="members-heading">
      <h3 id="members-heading">{messages.members}</h3>
      {failed && <p role="alert">{messages.somethingFailed}</p>}
      {!failed && members === null && <p>{messages.loading}</p>}
      {members !== null && (
        <ul aria-label={messages.members}>
          {members.map((member) => (
            <li key={member.person.personId}>
              {member.person.displayName} · {messages.roles[member.role]}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
