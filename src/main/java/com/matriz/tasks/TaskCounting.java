package com.matriz.tasks;

import java.util.Collection;
import java.util.Map;

import com.matriz.projects.ProjectId;

/**
 * The number of tasks of each project in each state, for Progress (FR-012, FR-024; architecture,
 * module rule 2). Deleted tasks are not counted (Assumptions A-006, A-022), and no task filter
 * applies (EX-026). Called inside the transaction of the use case that asks.
 */
public interface TaskCounting {

	/**
	 * The counts of each of the projects, in one query. A project with no tasks, or that does not
	 * exist, has zero counts.
	 * @return one entry for each of the projects
	 */
	Map<ProjectId, TaskCounts> countByState(Collection<ProjectId> projects);

	/** The number of tasks of one project in each state. */
	record TaskCounts(long toDo, long inProgress, long done) {

		/** No task in any state. */
		public static final TaskCounts NONE = new TaskCounts(0, 0, 0);

	}

}
