package com.matriz.tasks.application;

import java.util.List;

import com.matriz.identity.PersonId;
import com.matriz.projects.AccessDecision;
import com.matriz.projects.ProjectAccess;
import com.matriz.projects.ProjectAccess.ProjectFacts;
import com.matriz.projects.ProjectId;
import com.matriz.tasks.TaskAccessPolicy;
import com.matriz.tasks.TaskAction;
import com.matriz.tasks.TaskId;
import com.matriz.tasks.TaskPriority;
import com.matriz.tasks.application.InvalidTaskException.InvalidField;
import com.matriz.tasks.domain.Task;
import com.matriz.tasks.domain.TaskDescription;
import com.matriz.tasks.domain.TaskEdit;
import com.matriz.tasks.domain.TaskRepository;
import com.matriz.tasks.domain.TaskTitle;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edits a task's title, description, and priority (FR-009, FR-018; US-011, US-018; Assumptions A-001,
 * A-011, A-021, A-033). The checks come in the order of ADR-004 decision 5: 404, 403, 422, 409. The
 * project's row is locked for the whole use case, so simultaneous changes happen one after the other,
 * and the last one prevails (ADR-006; A-029).
 */
@Service
public class UpdateTask {

	private final ProjectAccess projects;

	private final TaskRepository tasks;

	private final TaskAccessPolicy policy;

	private final TaskViews views;

	UpdateTask(ProjectAccess projects, TaskRepository tasks, TaskAccessPolicy policy, TaskViews views) {
		this.projects = projects;
		this.tasks = tasks;
		this.policy = policy;
		this.views = views;
	}

	/**
	 * Changes the task.
	 * @param title the new title, or {@code null} to leave it unchanged
	 * @param changesDescription whether the description is given
	 * @param description the new description, or {@code null} to remove it; empty after trimming
	 * also removes it
	 * @param priority the code of the new priority, or {@code null} to leave it unchanged
	 * @throws TaskNotFoundException if there is no such project or task, or the person is not a member
	 * @throws NotProjectOwnerException if the person is a Project Participant
	 * @throws InvalidTaskException if the title is empty or too long, the description is too long, or
	 * the priority is not a code
	 * @throws ProjectCompletedException if the project is Completed
	 */
	@Transactional
	public TaskDetails update(PersonId person, ProjectId project, TaskId id, String title,
			boolean changesDescription, String description, String priority) {
		ProjectFacts facts = this.projects.findForUpdate(project, person).orElseThrow(TaskNotFoundException::ofTask);
		AccessDecision decision = this.policy.decide(TaskAction.EDIT, facts.state(), facts.role());
		TaskRules.requireMemberForTask(decision);
		Task task = this.tasks.findById(project, id).orElseThrow(TaskNotFoundException::ofTask);
		TaskRules.requirePermission(decision);
		List<InvalidField> invalid = TaskRules.noProblems();
		TaskTitle newTitle = TaskRules.title(title, invalid);
		TaskDescription newDescription = changesDescription ? TaskRules.description(description, invalid) : null;
		TaskPriority newPriority = TaskRules.priority(priority, invalid);
		TaskRules.requireNoProblems(invalid);
		TaskRules.requireNoConflict(decision);
		task.edit(new TaskEdit(newTitle, changesDescription, newDescription, newPriority));
		return this.views.of(task);
	}

}
