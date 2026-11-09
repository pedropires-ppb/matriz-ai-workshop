package com.matriz.projects.application;

import com.matriz.identity.PersonId;
import com.matriz.projects.AccessDecision;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectAccessPolicy;
import com.matriz.projects.ProjectAction;
import com.matriz.projects.ProjectId;
import com.matriz.projects.ProjectProgress;
import com.matriz.projects.domain.Project;
import com.matriz.projects.domain.ProjectRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shows one project to a member, also a Completed one (FR-001; US-007; Assumption A-011). */
@Service
public class GetProject {

	private final ProjectRepository projects;

	private final ProjectProgress progress;

	private final ProjectAccessPolicy policy;

	GetProject(ProjectRepository projects, ProjectProgress progress, ProjectAccessPolicy policy) {
		this.projects = projects;
		this.progress = progress;
		this.policy = policy;
	}

	/** @throws ProjectNotFoundException if there is no such project, or the person is not a member */
	@Transactional(readOnly = true)
	public ProjectDetails get(PersonId person, ProjectId id) {
		Project project = this.projects.findById(id).orElseThrow(ProjectNotFoundException::new);
		AccessDecision decision = this.policy.decide(ProjectAction.VIEW, project.state(), project.roleOf(person));
		ProjectRules.requireMemberWithPermission(decision);
		MemberRole role = project.roleOf(person).orElseThrow(ProjectNotFoundException::new);
		return ProjectDetails.of(project, role, this.progress.of(id));
	}

}
