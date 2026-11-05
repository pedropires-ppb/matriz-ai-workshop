package com.matriz.tasks.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.matriz.projects.ProjectId;
import com.matriz.tasks.TaskId;
import com.matriz.tasks.TaskPriority;
import com.matriz.tasks.TaskState;

/**
 * The tasks (ADR-008 decision 4). Every read path hides deleted tasks (Assumption A-022). A task is
 * always asked for together with its project, so that a task of another project is not found.
 */
public interface TaskRepository {

	Task save(Task task);

	/** The task of that project, unless it does not exist, belongs to another project, or is deleted. */
	Optional<Task> findById(ProjectId project, TaskId id);

	/**
	 * A page of the tasks of the project, not deleted, in the order they were created, oldest first
	 * (Assumption A-032, as amended). A task matches if its state is any of the states and its priority
	 * any of the priorities (Assumption A-024, as amended).
	 * @param states the states to include, or empty for all
	 * @param priorities the priorities to include, or empty for all
	 * @param page the number of the page, from 1
	 * @param size the maximum number of tasks in the page
	 */
	TaskPage find(ProjectId project, Set<TaskState> states, Set<TaskPriority> priorities, int page, int size);

	/** A page of tasks and the number of all the tasks that match. */
	record TaskPage(List<Task> items, long totalItems) {

	}

}
