package com.matriz.tasks.application;

import java.util.List;

import com.matriz.identity.PersonId;
import com.matriz.projects.AccessDecision;
import com.matriz.projects.ProjectAccess;
import com.matriz.projects.ProjectAccess.ProjectFacts;
import com.matriz.projects.ProjectId;
import com.matriz.tasks.TaskAccessPolicy;
import com.matriz.tasks.TaskAction;
import com.matriz.tasks.TaskFacts;
import com.matriz.tasks.TaskId;
import com.matriz.tasks.application.InvalidTaskException.InvalidField;
import com.matriz.tasks.domain.Task;
import com.matriz.tasks.domain.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assigns a member of a project to one of its tasks, in addition to the task's assignees (FR-010,
 * FR-019; US-012, US-020; Assumptions A-019, A-020, A-035). The checks come in the order of ADR-004
 * decision 5 for this operation: 404, 403, 422 for a person who is not a member or does not exist,
 * 409. The project's row is locked for the whole use case (ADR-006), so two requests for the same
 * person, or a removal of the person's membership, wait for each other.
 */
@Service
public class AssignTask {

	static final String NOT_A_MEMBER = "Only members of the project can be assigned to its tasks.";

	static final String NOT_A_MEMBER_FIELD = "This person is not a member of the project.";

	private final ProjectAccess projects;

	private final TaskRepository tasks;

	private final TaskAccessPolicy policy;

	private final TaskViews views;

	AssignTask(ProjectAccess projects, TaskRepository tasks, TaskAccessPolicy policy, TaskViews views) {
		this.projects = projects;
		this.tasks = tasks;
		this.policy = policy;
		this.views = views;
	}

	/**
	 * Assigns the person; assigning a person who is already an assignee changes nothing.
	 * @param person the person who asks, from the token
	 * @param assignee the identifier of the person to assign, as sent
	 * @throws TaskNotFoundException if there is no such project or task, or the person is not a member
	 * @throws NotProjectOwnerException if the person is a Project Participant and names someone else
	 * @throws InvalidTaskException if the person named is not a member of the project, or does not exist
	 * @throws ProjectCompletedException if the project is Completed
	 */
	@Transactional
	public TaskDetails assign(PersonId person, ProjectId project, TaskId id, String assignee) {
		ProjectFacts facts = this.projects.findForUpdate(project, person).orElseThrow(TaskNotFoundException::ofTask);
		TaskRules.requireMemberForTask(this.policy.decide(TaskAction.VIEW, facts.state(), facts.role()));
		Task task = this.tasks.findById(project, id).orElseThrow(TaskNotFoundException::ofTask);
		PersonId named = TaskRules.personId(assignee);
		AccessDecision decision = this.policy.decide(TaskAction.ASSIGN, facts.state(), facts.role(),
				new TaskFacts(false, person.equals(named)));
		TaskRules.requirePermission(decision, TaskAction.ASSIGN);
		if (named == null || this.projects.find(project, named).flatMap(ProjectFacts::role).isEmpty()) {
			throw new InvalidTaskException(NOT_A_MEMBER, List.of(new InvalidField("/personId", NOT_A_MEMBER_FIELD)));
		}
		TaskRules.requireNoConflict(decision);
		task.assign(named);
		return this.views.of(task);
	}

}
