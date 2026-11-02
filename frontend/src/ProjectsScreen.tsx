import { type FormEvent, useCallback, useEffect, useState } from 'react';
import { type Session, SessionEndedError } from './api';
import { messages } from './messages';
import ProjectForm from './ProjectForm';
import { createProject, listProjects, type ProjectPage } from './projectsApi';

const PAGE_SIZE = 20;

interface Props {
  session: Session;
  onOpen: (projectId: string) => void;
  onSessionEnded: () => void;
}

/** My projects (US-007, US-023): the person's projects, a page at a time, searchable by name, and the form to create one. */
export default function ProjectsScreen({ session, onOpen, onSessionEnded }: Props) {
  const [searchText, setSearchText] = useState('');
  const [name, setName] = useState('');
  const [page, setPage] = useState(1);
  const [data, setData] = useState<ProjectPage | null>(null);
  const [failed, setFailed] = useState(false);
  const [creating, setCreating] = useState(false);

  const load = useCallback(async () => {
    try {
      const result = await listProjects(session, { name, page, size: PAGE_SIZE });
      if (result.outcome === 'ok') {
        setData(result.value);
        setFailed(false);
      } else {
        setFailed(true);
      }
    } catch (error: unknown) {
      if (error instanceof SessionEndedError) {
        onSessionEnded();
      } else {
        setFailed(true);
      }
    }
  }, [session, name, page, onSessionEnded]);

  useEffect(() => {
    void load();
  }, [load]);

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setName(searchText.trim());
    setPage(1);
  }

  function clear() {
    setSearchText('');
    setName('');
    setPage(1);
  }

  async function create(content: { name: string; description: string }) {
    try {
      return await createProject(session, content);
    } catch (error: unknown) {
      if (error instanceof SessionEndedError) {
        onSessionEnded();
      }
      return { outcome: 'failed' } as const;
    }
  }

  return (
    <section aria-labelledby="my-projects-heading">
      <h2 id="my-projects-heading">{messages.myProjects}</h2>
      {creating ? (
        <section aria-labelledby="new-project-heading">
          <h3 id="new-project-heading">{messages.newProject}</h3>
          <ProjectForm
            idPrefix="new-project"
            initialName=""
            initialDescription=""
            submitLabel={messages.createProject}
            onSubmit={create}
            onSaved={(project) => onOpen(project.projectId)}
            onCancel={() => setCreating(false)}
          />
        </section>
      ) : (
        <p>
          <button type="button" onClick={() => setCreating(true)}>
            {messages.newProject}
          </button>
        </p>
      )}
      <form role="search" onSubmit={search}>
        <label htmlFor="project-search">{messages.searchByName}</label>{' '}
        <input id="project-search" type="search" value={searchText} onChange={(event) => setSearchText(event.target.value)} />{' '}
        <button type="submit">{messages.search}</button>
        {name !== '' && (
          <>
            {' '}
            <button type="button" onClick={clear}>
              {messages.clearSearch}
            </button>
          </>
        )}
      </form>
      {failed && <p role="alert">{messages.somethingFailed}</p>}
      {data === null ? (
        !failed && <p>{messages.loading}</p>
      ) : data.items.length === 0 ? (
        <p>{name === '' ? messages.noProjects : messages.noProjectMatches}</p>
      ) : (
        <ul aria-label={messages.myProjects}>
          {data.items.map((project) => (
            <li key={project.projectId}>
              <button type="button" onClick={() => onOpen(project.projectId)}>
                {project.name}
              </button>{' '}
              <span>
                {messages.states[project.state]}, {messages.roles[project.myRole]}
              </span>
              {project.description !== undefined && <p>{project.description}</p>}
            </li>
          ))}
        </ul>
      )}
      {data !== null && data.totalPages > 1 && (
        <nav aria-label="Pages">
          <button type="button" disabled={data.page <= 1} onClick={() => setPage(data.page - 1)}>
            {messages.previous}
          </button>{' '}
          <span>{messages.pageOf(data.page, data.totalPages)}</span>{' '}
          <button type="button" disabled={data.page >= data.totalPages} onClick={() => setPage(data.page + 1)}>
            {messages.next}
          </button>
        </nav>
      )}
    </section>
  );
}
