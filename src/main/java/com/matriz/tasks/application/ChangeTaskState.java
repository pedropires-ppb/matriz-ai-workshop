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
import com.matriz.tasks.TaskState;
import com.matriz.tasks.application.InvalidTaskException.InvalidField;
import com.matriz.tasks.domain.Task;
import com.matriz.tasks.domain.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sets a task's state, from any state to any state (FR-011; US-005; Assumptions A-005, A-011, A-019,
 * A-029, A-033). The Project Owner or an assignee may do it (A-019, A-035). The checks come in the
 * order of ADR-004 decision 5: 404, 403, 422, 409. The project's row is locked for the whole use case
 * (ADR-006).
 */
@Service
public class ChangeTaskState {

	private final ProjectAccess projects;

	private final TaskRepository tasks;

	private final TaskAccessPolicy policy;

	private final TaskViews views;

	ChangeTaskState(ProjectAccess projects, TaskRepository tasks, TaskAccessPolicy policy, TaskViews views) {
		this.projects = projects;
		this.tasks = tasks;
		this.policy = policy;
		this.views = views;
	}

	/**
	 * Sets the state; setting the state the task already has changes nothing.
	 * @param state the code of the state
	 * @throws TaskNotFoundException if there is no such project or task, or the person is not a member
	 * @throws NotProjectOwnerException if the person is neither the Project Owner nor an assignee
	 * @throws InvalidTaskException if the state is not a code
	 * @throws ProjectCompletedException if the project is Completed
	 */
	@Transactional
	public TaskDetails change(PersonId person, ProjectId project, TaskId id, String state) {
		ProjectFacts facts = this.projects.findForUpdate(project, person).orElseThrow(TaskNotFoundException::ofTask);
		AccessDecision membership = this.policy.decide(TaskAction.VIEW, facts.state(), facts.role());
		TaskRules.requireMemberForTask(membership);
		Task task = this.tasks.findById(project, id).orElseThrow(TaskNotFoundException::ofTask);
		AccessDecision decision = this.policy.decide(TaskAction.CHANGE_STATE, facts.state(), facts.role(),
				new TaskFacts(task.assignees().contains(person), false));
		TaskRules.requirePermission(decision, TaskAction.CHANGE_STATE);
		List<InvalidField> invalid = TaskRules.noProblems();
		TaskState newState = TaskRules.state(state, invalid);
		TaskRules.requireNoProblems(invalid);
		TaskRules.requireNoConflict(decision);
		task.changeState(newState);
		return this.views.of(task);
	}

}
