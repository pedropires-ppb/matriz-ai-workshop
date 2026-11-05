package com.matriz.projects.application;

import java.util.Optional;

import com.matriz.identity.PersonId;
import com.matriz.projects.ProjectAccess;
import com.matriz.projects.ProjectId;
import com.matriz.projects.domain.Project;
import com.matriz.projects.domain.ProjectRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gives other modules the facts about a project that their access decisions need. It joins the
 * transaction of the use case that calls it: a lock is held until that transaction ends (ADR-006).
 */
@Component
class ProjectFactsProvider implements ProjectAccess {

	private final ProjectRepository projects;

	ProjectFactsProvider(ProjectRepository projects) {
		this.projects = projects;
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
	public Optional<ProjectFacts> find(ProjectId project, PersonId person) {
		return this.projects.findById(project).map((found) -> facts(found, person));
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public Optional<ProjectFacts> findForUpdate(ProjectId project, PersonId person) {
		return this.projects.findByIdForUpdate(project).map((found) -> facts(found, person));
	}

	private static ProjectFacts facts(Project project, PersonId person) {
		return new ProjectFacts(project.state(), project.roleOf(person));
	}

}
