import { EmptyState, Link, LoadFailed, Loading, Notice, StateBadge } from './components';
import { messages } from './messages';
import { type Flash, useFlash } from './navigation';
import { listProjects } from './projectsApi';
import { ALL_PROJECTS, FIRST_PAGE, NEW_PROJECT } from './router';
import { useLoad, useSession } from './session';
import { refusal } from './validation';

/** How many projects Home lists; "All projects" opens the rest (I13 decision 1). */
const HOME_PROJECTS = 5;

/**
 * Home (I13 decision 1): the signed-in person, how many projects they can see, from the API's total, and
 * the first of them. The task counts by state come with I5.
 */
export default function HomeScreen({ notice }: { notice: Flash | null }) {
  const { session } = useSession();
  const [shown] = useFlash(notice);
  const projects = useLoad((current) => listProjects(current, { name: '', page: 1, size: HOME_PROJECTS }), []);

  return (
    <section aria-labelledby="home-heading" className="screen">
      <h2 id="home-heading">{messages.home}</h2>
      <p>{messages.welcome(session.person.displayName)}</p>
      {shown !== null && <Notice kind="success">{shown}</Notice>}
      <section aria-labelledby="home-projects-heading" className="panel">
        <h3 id="home-projects-heading">{messages.myProjects}</h3>
        {projects.state.status === 'loading' && <Loading />}
        {projects.state.status === 'failed' && (
          <LoadFailed text={refusal(projects.state.result, 'project')} onRetry={projects.retry} />
        )}
        {projects.state.status === 'ready' &&
          (projects.state.value.totalItems === 0 ? (
            <EmptyState text={messages.noProjects}>
              <Link to={NEW_PROJECT} className="button primary">
                {messages.createAProject}
              </Link>
            </EmptyState>
          ) : (
            <>
              <p className="count">{messages.projectCount(projects.state.value.totalItems)}</p>
              <ul aria-label={messages.myProjects} className="rows">
                {projects.state.value.items.map((project) => (
                  <li key={project.projectId}>
                    <Link to={{ screen: 'project', projectId: project.projectId, list: FIRST_PAGE }}>{project.name}</Link>
                    <span className="badges">
                      <StateBadge kind="project" value={project.state} />
                      <StateBadge kind="role" value={project.myRole} />
                    </span>
                  </li>
                ))}
              </ul>
              <p>
                <Link to={ALL_PROJECTS}>{messages.allProjects}</Link>
              </p>
            </>
          ))}
      </section>
    </section>
  );
}
