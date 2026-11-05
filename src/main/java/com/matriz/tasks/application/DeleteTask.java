package com.matriz.tasks.application;

import java.time.Instant;

import com.matriz.identity.PersonId;
import com.matriz.projects.AccessDecision;
import com.matriz.projects.ProjectAccess;
import com.matriz.projects.ProjectAccess.ProjectFacts;
import com.matriz.projects.ProjectId;
import com.matriz.tasks.TaskAccessPolicy;
import com.matriz.tasks.TaskAction;
import com.matriz.tasks.TaskId;
import com.matriz.tasks.domain.Task;
import com.matriz.tasks.domain.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes a task (FR-017; US-017; Assumptions A-001, A-011, A-022). It is a soft deletion: the task is
 * hidden from everyone, its assignments are removed, and its other data, such as comments, is kept, so
 * no record is orphaned. The project's row is locked for the whole use case (ADR-006). The
 * confirmation is a step of the User interface; this use case runs only after it.
 */
@Service
public class DeleteTask {

	private final ProjectAccess projects;

	private final TaskRepository tasks;

	private final TaskAccessPolicy policy;

	DeleteTask(ProjectAccess projects, TaskRepository tasks, TaskAccessPolicy policy) {
		this.projects = projects;
		this.tasks = tasks;
		this.policy = policy;
	}

	/**
	 * @throws TaskNotFoundException if there is no such project or task, it is already deleted, or the
	 * person is not a member
	 * @throws NotProjectOwnerException if the person is a Project Participant
	 * @throws ProjectCompletedException if the project is Completed
	 */
	@Transactional
	public void delete(PersonId person, ProjectId project, TaskId id) {
		ProjectFacts facts = this.projects.findForUpdate(project, person).orElseThrow(TaskNotFoundException::ofTask);
		AccessDecision decision = this.policy.decide(TaskAction.DELETE, facts.state(), facts.role());
		TaskRules.requireMemberForTask(decision);
		Task task = this.tasks.findById(project, id).orElseThrow(TaskNotFoundException::ofTask);
		TaskRules.requirePermission(decision);
		TaskRules.requireNoConflict(decision);
		task.delete(Instant.now());
	}

}
