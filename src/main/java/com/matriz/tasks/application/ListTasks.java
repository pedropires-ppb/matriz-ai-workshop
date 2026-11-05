package com.matriz.tasks.application;

import java.util.List;
import java.util.Set;

import com.matriz.identity.PersonId;
import com.matriz.projects.AccessDecision;
import com.matriz.projects.ProjectAccess;
import com.matriz.projects.ProjectAccess.ProjectFacts;
import com.matriz.projects.ProjectId;
import com.matriz.tasks.TaskAccessPolicy;
import com.matriz.tasks.TaskAction;
import com.matriz.tasks.TaskPriority;
import com.matriz.tasks.TaskState;
import com.matriz.tasks.domain.TaskRepository;
import com.matriz.tasks.domain.TaskRepository.TaskPage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lists the tasks of a project to a member, a page at a time, oldest first, never deleted ones, also
 * in a Completed project (FR-006, FR-023; US-004, US-024; Assumptions A-011, A-022, A-024, A-032).
 * Filters change only this list: they never change progress (EX-026).
 */
@Service
public class ListTasks {

	private final ProjectAccess projects;

	private final TaskRepository tasks;

	private final TaskAccessPolicy policy;

	private final TaskViews views;

	ListTasks(ProjectAccess projects, TaskRepository tasks, TaskAccessPolicy policy, TaskViews views) {
		this.projects = projects;
		this.tasks = tasks;
		this.policy = policy;
		this.views = views;
	}

	/**
	 * A page of tasks.
	 * @param states the states to include, or empty for all
	 * @param priorities the priorities to include, or empty for all
	 * @param page the number of the page, from 1
	 * @param size the maximum number of tasks in the page
	 * @throws TaskNotFoundException if there is no such project, or the person is not a member
	 */
	@Transactional(readOnly = true)
	public TasksPage list(PersonId person, ProjectId project, Set<TaskState> states, Set<TaskPriority> priorities,
			int page, int size) {
		ProjectFacts facts = this.projects.find(project, person).orElseThrow(TaskNotFoundException::ofProject);
		AccessDecision decision = this.policy.decide(TaskAction.VIEW, facts.state(), facts.role());
		TaskRules.requireMemberOfProject(decision);
		TaskPage found = this.tasks.find(project, states, priorities, page, size);
		List<TaskDetails> items = this.views.of(found.items());
		long totalPages = (found.totalItems() + size - 1) / size;
		return new TasksPage(items, page, size, found.totalItems(), totalPages);
	}

}
