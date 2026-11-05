package com.matriz.projects.application;

import java.time.Instant;

import com.matriz.identity.PersonId;
import com.matriz.projects.ProjectAccessPolicy;
import com.matriz.projects.ProjectAction;
import com.matriz.projects.ProjectId;
import com.matriz.projects.domain.Project;
import com.matriz.projects.domain.ProjectRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes a project (FR-016; US-016; Assumptions A-001, A-011, A-022). It is a soft deletion: the
 * project, with its members, tasks, assignments, and comments, is hidden from everyone, and no data
 * is removed, so no record is orphaned. The project's row is locked for the whole use case
 * (ADR-006). The confirmation is a step of the User interface; this use case runs only after it.
 */
@Service
public class DeleteProject {

	private final ProjectRepository projects;

	private final ProjectAccessPolicy policy;

	DeleteProject(ProjectRepository projects, ProjectAccessPolicy policy) {
		this.projects = projects;
		this.policy = policy;
	}

	/**
	 * @throws ProjectNotFoundException if there is no such project, it is already deleted, or the
	 * person is not a member
	 * @throws NotProjectOwnerException if the person is a Project Participant
	 * @throws ProjectCompletedException if the project is Completed
	 */
	@Transactional
	public void delete(PersonId person, ProjectId id) {
		Project project = this.projects.findByIdForUpdate(id).orElseThrow(ProjectNotFoundException::new);
		var decision = this.policy.decide(ProjectAction.DELETE, project.state(), project.roleOf(person));
		ProjectRules.requireMemberWithPermission(decision);
		ProjectRules.requireNoConflict(decision);
		project.delete(person, Instant.now());
	}

}
