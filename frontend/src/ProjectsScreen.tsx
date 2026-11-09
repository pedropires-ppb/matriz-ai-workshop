import { type FormEvent, useEffect, useState } from 'react';
import { Button, EmptyState, Link, LoadFailed, Loading, Notice, Pagination, ProjectRow } from './components';
import { messages } from './messages';
import { type Flash, go, useFlash } from './navigation';
import ProjectForm from './ProjectForm';
import { createProject, listProjects } from './projectsApi';
import { ALL_PROJECTS, FIRST_PAGE, NEW_PROJECT, type ProjectsRoute } from './router';
import { useApi, useLoad } from './session';
import { refusal } from './validation';

const PAGE_SIZE = 20;

/**
 * Projects (US-007, US-023; I13 decision 1): the person's projects, a page at a time, searchable by name,
 * and the form to create one. The search and the page are in the address, and the total is the API's. Each
 * project shows its task counts by state (I5 decision 4).
 */
export default function ProjectsScreen({ route, notice }: { route: ProjectsRoute; notice: Flash | null }) {
  const { run } = useApi();
  const [shown, setShown] = useFlash(notice);
  const [searchText, setSearchText] = useState(route.name);
  const { name, page, creating } = route;
  const projects = useLoad((session) => listProjects(session, { name, page, size: PAGE_SIZE }), [name, page]);

  useEffect(() => setSearchText(name), [name]);

  // A page past the last one, for example after the last project of a page was deleted, shows the last page.
  useEffect(() => {
    if (projects.state.status === 'ready' && projects.state.value.items.length === 0 && page > 1) {
      go({ ...route, page: Math.max(1, projects.state.value.totalPages) }, { replace: true });
    }
  }, [projects.state, page, route]);

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    go({ ...ALL_PROJECTS, name: searchText.trim() });
  }

  return (
    <section aria-labelledby="projects-heading" className="screen">
      <h2 id="projects-heading">{messages.projects}</h2>
      {shown !== null && <Notice kind="success">{shown}</Notice>}
      {creating ? (
        <section aria-labelledby="new-project-heading" className="panel">
          <h3 id="new-project-heading">{messages.newProject}</h3>
          <ProjectForm
            idPrefix="new-project"
            initial={{ name: '', description: '' }}
            submitLabel={messages.createProject}
            onSubmit={(content) => run((session) => createProject(session, content))}
            onSaved={(project) =>
              go(
                { screen: 'project', projectId: project.projectId, list: FIRST_PAGE },
                { replace: true, notice: messages.projectCreated },
              )
            }
            onCancel={() => go(ALL_PROJECTS, { replace: true })}
          />
        </section>
      ) : (
        <p>
          <Link to={NEW_PROJECT} className="button primary">
            {messages.newProject}
          </Link>
        </p>
      )}
      <form role="search" onSubmit={search} className="search">
        <label htmlFor="project-search">{messages.searchByName}</label>
        <input id="project-search" type="search" value={searchText} onChange={(event) => setSearchText(event.target.value)} />
        <Button type="submit">{messages.search}</Button>
        {name !== '' && (
          <Button
            onClick={() => {
              setShown(null);
              go(ALL_PROJECTS);
            }}
          >
            {messages.clearSearch}
          </Button>
        )}
      </form>
      {projects.state.status === 'loading' && <Loading />}
      {projects.state.status === 'failed' && (
        <LoadFailed text={refusal(projects.state.result, 'project')} onRetry={projects.retry} />
      )}
      {projects.state.status === 'ready' && (
        <>
          <p className="count" aria-live="polite">
            {messages.projectCount(projects.state.value.totalItems)}
          </p>
          {projects.state.value.totalItems === 0 ? (
            name === '' ? (
              <EmptyState text={messages.noProjects}>
                {!creating && (
                  <Link to={NEW_PROJECT} className="button primary">
                    {messages.createAProject}
                  </Link>
                )}
              </EmptyState>
            ) : (
              <EmptyState text={messages.noProjectMatches} />
            )
          ) : (
            <ul aria-label={messages.projects} className="rows">
              {projects.state.value.items.map((project) => (
                <ProjectRow key={project.projectId} project={project} withDescription />
              ))}
            </ul>
          )}
          <Pagination
            page={projects.state.value.page}
            totalPages={projects.state.value.totalPages}
            onPage={(next) => go({ ...route, page: next })}
          />
        </>
      )}
    </section>
  );
}
