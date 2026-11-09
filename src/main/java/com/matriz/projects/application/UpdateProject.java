package com.matriz.projects.application;

import java.util.List;

import com.matriz.identity.PersonId;
import com.matriz.projects.AccessDecision;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectAccessPolicy;
import com.matriz.projects.ProjectAction;
import com.matriz.projects.ProjectId;
import com.matriz.projects.ProjectProgress;
import com.matriz.projects.application.InvalidProjectException.InvalidField;
import com.matriz.projects.domain.Project;
import com.matriz.projects.domain.ProjectDescription;
import com.matriz.projects.domain.ProjectEdit;
import com.matriz.projects.domain.ProjectName;
import com.matriz.projects.domain.ProjectRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edits a project's name and description (FR-003; US-008; Assumptions A-001, A-011, A-031). The
 * checks come in the order of ADR-004 decision 5: 404, 403, 422, 409. The project's row is locked
 * for the whole use case, so simultaneous changes to it happen one after the other (ADR-006).
 */
@Service
public class UpdateProject {

	private final ProjectRepository projects;

	private final ProjectProgress progress;

	private final ProjectAccessPolicy policy;

	UpdateProject(ProjectRepository projects, ProjectProgress progress, ProjectAccessPolicy policy) {
		this.projects = projects;
		this.progress = progress;
		this.policy = policy;
	}

	/**
	 * Checks only what comes before the content (404 and 403), so that the API answers those before it
	 * reads the request's content (ADR-004 decision 5). {@link #update} checks them again.
	 */
	@Transactional(readOnly = true)
	public void checkCanEdit(PersonId person, ProjectId id) {
		Project project = this.projects.findById(id).orElseThrow(ProjectNotFoundException::new);
		ProjectRules.requireMemberWithPermission(
				this.policy.decide(ProjectAction.EDIT, project.state(), project.roleOf(person)));
	}

	/**
	 * Changes the project.
	 * @param name the new name, or {@code null} to leave it unchanged
	 * @param changesDescription whether the description is given
	 * @param description the new description, or {@code null} to remove it; empty after trimming
	 * also removes it
	 * @throws ProjectNotFoundException if there is no such project, or the person is not a member
	 * @throws NotProjectOwnerException if the person is a Project Participant
	 * @throws InvalidProjectException if the name is empty or too long, or the description is too long
	 * @throws ProjectCompletedException if the project is Completed
	 */
	@Transactional
	public ProjectDetails update(PersonId person, ProjectId id, String name, boolean changesDescription,
			String description) {
		Project project = this.projects.findByIdForUpdate(id).orElseThrow(ProjectNotFoundException::new);
		AccessDecision decision = this.policy.decide(ProjectAction.EDIT, project.state(), project.roleOf(person));
		ProjectRules.requireMemberWithPermission(decision);
		List<InvalidField> invalid = ProjectRules.noProblems();
		ProjectName newName = ProjectRules.name(name, invalid);
		ProjectDescription newDescription = changesDescription ? ProjectRules.description(description, invalid)
				: null;
		ProjectRules.requireNoProblems(invalid);
		ProjectRules.requireNoConflict(decision);
		project.edit(person, new ProjectEdit(newName, changesDescription, newDescription));
		return ProjectDetails.of(project, MemberRole.PROJECT_OWNER, this.progress.of(id));
	}

}
