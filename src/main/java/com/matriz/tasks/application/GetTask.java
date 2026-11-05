package com.matriz.tasks.application;

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

/** Shows one task to a member of its project, also in a Completed project (FR-006; US-004; Assumption A-011). */
@Service
public class GetTask {

	private final ProjectAccess projects;

	private final TaskRepository tasks;

	private final TaskAccessPolicy policy;

	private final TaskViews views;

	GetTask(ProjectAccess projects, TaskRepository tasks, TaskAccessPolicy policy, TaskViews views) {
		this.projects = projects;
		this.tasks = tasks;
		this.policy = policy;
		this.views = views;
	}

	/** @throws TaskNotFoundException if there is no such project or task, or the person is not a member */
	@Transactional(readOnly = true)
	public TaskDetails get(PersonId person, ProjectId project, TaskId id) {
		ProjectFacts facts = this.projects.find(project, person).orElseThrow(TaskNotFoundException::ofTask);
		AccessDecision decision = this.policy.decide(TaskAction.VIEW, facts.state(), facts.role());
		TaskRules.requireMemberForTask(decision);
		Task task = this.tasks.findById(project, id).orElseThrow(TaskNotFoundException::ofTask);
		return this.views.of(task);
	}

}
