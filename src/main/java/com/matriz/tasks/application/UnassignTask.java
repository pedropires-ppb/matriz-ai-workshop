package com.matriz.tasks.application;

import com.matriz.identity.PersonId;
import com.matriz.projects.AccessDecision;
import com.matriz.projects.ProjectAccess;
import com.matriz.projects.ProjectAccess.ProjectFacts;
import com.matriz.projects.ProjectId;
import com.matriz.tasks.TaskAccessPolicy;
import com.matriz.tasks.TaskAction;
import com.matriz.tasks.TaskFacts;
import com.matriz.tasks.TaskId;
import com.matriz.tasks.domain.Task;
import com.matriz.tasks.domain.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Removes an assignee from a task (FR-010, FR-019; US-019, US-020; Assumptions A-019, A-020, A-035).
 * The checks come in the order of ADR-004 decision 5: 404 (also for a person who is not an assignee),
 * 403, 409. The project's row is locked for the whole use case (ADR-006). The last assignee can be
 * removed, and the task's state is not changed.
 */
@Service
public class UnassignTask {

	private final ProjectAccess projects;

	private final TaskRepository tasks;

	private final TaskAccessPolicy policy;

	private final TaskViews views;

	UnassignTask(ProjectAccess projects, TaskRepository tasks, TaskAccessPolicy policy, TaskViews views) {
		this.projects = projects;
		this.tasks = tasks;
		this.policy = policy;
		this.views = views;
	}

	/**
	 * @param person the person who asks, from the token
	 * @param assignee the identifier of the assignee to remove, as sent
	 * @throws TaskNotFoundException if there is no such project, task, or assignee, or the person is
	 * not a member
	 * @throws NotProjectOwnerException if the person is a Project Participant and names someone else
	 * @throws ProjectCompletedException if the project is Completed
	 */
	@Transactional
	public TaskDetails unassign(PersonId person, ProjectId project, TaskId id, String assignee) {
		ProjectFacts facts = this.projects.findForUpdate(project, person).orElseThrow(TaskNotFoundException::ofTask);
		TaskRules.requireMemberForTask(this.policy.decide(TaskAction.VIEW, facts.state(), facts.role()));
		Task task = this.tasks.findById(project, id).orElseThrow(TaskNotFoundException::ofTask);
		PersonId named = TaskRules.personId(assignee);
		if (named == null || !task.assignees().contains(named)) {
			throw TaskNotFoundException.ofAssignee();
		}
		AccessDecision decision = this.policy.decide(TaskAction.UNASSIGN, facts.state(), facts.role(),
				new TaskFacts(false, person.equals(named)));
		TaskRules.requirePermission(decision, TaskAction.UNASSIGN);
		TaskRules.requireNoConflict(decision);
		task.unassign(named);
		return this.views.of(task);
	}

}
