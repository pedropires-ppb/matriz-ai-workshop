package com.matriz.tasks.application;

import com.matriz.identity.PersonId;
import com.matriz.projects.AccessDecision;
import com.matriz.projects.ProjectAccess;
import com.matriz.projects.ProjectAccess.ProjectFacts;
import com.matriz.projects.ProjectId;
import com.matriz.tasks.TaskAccessPolicy;
import com.matriz.tasks.TaskAction;
import com.matriz.tasks.TaskId;
import com.matriz.tasks.domain.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks only what comes before the content of a request (404 and 403), so that the API answers
 * those before it reads the content (ADR-004 decision 5). The use cases that change a task check them
 * again, with the project's row locked.
 */
@Service
public class CheckTaskAccess {

	private final ProjectAccess projects;

	private final TaskRepository tasks;

	private final TaskAccessPolicy policy;

	CheckTaskAccess(ProjectAccess projects, TaskRepository tasks, TaskAccessPolicy policy) {
		this.projects = projects;
		this.tasks = tasks;
		this.policy = policy;
	}

	/**
	 * @param task the task the action is for, or {@code null} for an action on the project's tasks
	 * @throws TaskNotFoundException if there is no such project or task, or the person is not a member
	 * @throws NotProjectOwnerException if the person is a Project Participant
	 */
	@Transactional(readOnly = true)
	public void check(PersonId person, ProjectId project, TaskId task, TaskAction action) {
		ProjectFacts facts = this.projects.find(project, person)
			.orElseThrow(() -> task == null ? TaskNotFoundException.ofProject() : TaskNotFoundException.ofTask());
		AccessDecision decision = this.policy.decide(action, facts.state(), facts.role());
		if (task == null) {
			TaskRules.requireMemberOfProject(decision);
		}
		else {
			TaskRules.requireMemberForTask(decision);
			this.tasks.findById(project, task).orElseThrow(TaskNotFoundException::ofTask);
		}
		TaskRules.requirePermission(decision);
	}

}
