package com.matriz.projects.application;

import com.matriz.identity.PersonId;
import com.matriz.projects.AccessDecision;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectAccessPolicy;
import com.matriz.projects.ProjectAction;
import com.matriz.projects.ProjectId;
import com.matriz.projects.ProjectProgress;
import com.matriz.projects.ProjectTasks;
import com.matriz.projects.domain.Project;
import com.matriz.projects.domain.ProjectRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Completes a project (FR-007; US-010; EX-006, EX-018; Assumptions A-001, A-007, A-011). The project's
 * row is locked for the whole use case, so the check for unfinished tasks and the change of state are
 * one indivisible step: a task created or reopened at the same time waits, and then finds the project
 * Completed (ADR-006; Assumption A-029). A project with no tasks can be completed (US-010 criterion 2,
 * pending the client's confirmation of A-007).
 */
@Service
public class CompleteProject {

	private final ProjectRepository projects;

	private final ProjectTasks tasks;

	private final ProjectProgress progress;

	private final ProjectAccessPolicy policy;

	CompleteProject(ProjectRepository projects, ProjectTasks tasks, ProjectProgress progress,
			ProjectAccessPolicy policy) {
		this.projects = projects;
		this.tasks = tasks;
		this.progress = progress;
		this.policy = policy;
	}

	/**
	 * Checks only what comes before the content (404 and 403), so that the API answers those before it
	 * reads the request's content (ADR-004 decision 5). {@link #complete} checks them again.
	 */
	@Transactional(readOnly = true)
	public void checkCanComplete(PersonId person, ProjectId id) {
		Project project = this.projects.findById(id).orElseThrow(ProjectNotFoundException::new);
		ProjectRules.requireMemberWithPermission(
				this.policy.decide(ProjectAction.COMPLETE, project.state(), project.roleOf(person)));
	}

	/**
	 * @throws ProjectNotFoundException if there is no such project, it is deleted, or the person is
	 * not a member
	 * @throws NotProjectOwnerException if the person is a Project Participant
	 * @throws ProjectCompletedException if the project is already Completed
	 * @throws UnfinishedTasksException if a task of the project is To do or In progress
	 */
	@Transactional
	public ProjectDetails complete(PersonId person, ProjectId id) {
		Project project = this.projects.findByIdForUpdate(id).orElseThrow(ProjectNotFoundException::new);
		AccessDecision decision = this.policy.decide(ProjectAction.COMPLETE, project.state(),
				project.roleOf(person));
		ProjectRules.requireMemberWithPermission(decision);
		ProjectRules.requireNoConflict(decision);
		boolean unfinished = this.tasks.hasUnfinishedTasks(id);
		if (unfinished) {
			throw new UnfinishedTasksException();
		}
		project.complete(person, unfinished);
		return ProjectDetails.of(project, MemberRole.PROJECT_OWNER, this.progress.of(id));
	}

}
