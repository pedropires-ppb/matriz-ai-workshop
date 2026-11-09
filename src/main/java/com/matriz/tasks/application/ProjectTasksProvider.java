package com.matriz.tasks.application;

import com.matriz.projects.ProjectId;
import com.matriz.projects.ProjectTasks;
import com.matriz.tasks.domain.TaskRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** The facts about a project's tasks that Projects asks for (ADR-008 decision 3). */
@Component
class ProjectTasksProvider implements ProjectTasks {

	private final TaskRepository tasks;

	ProjectTasksProvider(TaskRepository tasks) {
		this.tasks = tasks;
	}

	/** Runs only inside the caller's transaction, which has locked the project (ADR-006). */
	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public boolean hasUnfinishedTasks(ProjectId project) {
		return this.tasks.hasUnfinished(project);
	}

}
