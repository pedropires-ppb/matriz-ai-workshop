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
import com.matriz.tasks.domain.TaskRepository;
import com.matriz.tasks.domain.TaskTitle;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a task in an Active project (FR-008, FR-018; US-003, US-018; Assumptions A-001, A-010,
 * A-011, A-021, A-033). The checks come in the order of ADR-004 decision 5: 404, 403, 422, 409. The
 * project's row is locked for the whole use case (ADR-006).
 */
@Service
public class CreateTask {

	private final ProjectAccess projects;

	private final TaskRepository tasks;

	private final TaskAccessPolicy policy;

	private final TaskViews views;

	CreateTask(ProjectAccess projects, TaskRepository tasks, TaskAccessPolicy policy, TaskViews views) {
		this.projects = projects;
		this.tasks = tasks;
		this.policy = policy;
		this.views = views;
	}

	/**
	 * Creates a To do task with no assignee and stores it.
	 * @param description the text, or {@code null} for none; empty after trimming means none
	 * @param priority the code of the priority, or {@code null} for Medium
	 * @throws TaskNotFoundException if there is no such project, or the person is not a member
	 * @throws NotProjectOwnerException if the person is a Project Participant
	 * @throws InvalidTaskException if the title is empty or too long, the description is too long, or
	 * the priority is not a code
	 * @throws ProjectCompletedException if the project is Completed
	 */
	@Transactional
	public TaskDetails create(PersonId person, ProjectId project, String title, String description,
			String priority) {
		ProjectFacts facts = this.projects.findForUpdate(project, person).orElseThrow(TaskNotFoundException::ofProject);
		AccessDecision decision = this.policy.decide(TaskAction.CREATE, facts.state(), facts.role());
		TaskRules.requireMemberOfProject(decision);
		TaskRules.requirePermission(decision);
		List<InvalidField> invalid = TaskRules.noProblems();
		TaskTitle validTitle = TaskRules.title(title == null ? "" : title, invalid);
		TaskDescription validDescription = TaskRules.description(description, invalid);
		TaskPriority validPriority = TaskRules.priority(priority, invalid);
		TaskRules.requireNoProblems(invalid);
		TaskRules.requireNoConflict(decision);
		Task task = this.tasks.save(Task.create(TaskId.generate(), project, validTitle, validDescription,
				validPriority == null ? TaskPriority.MEDIUM : validPriority));
		return this.views.of(task);
	}

}
