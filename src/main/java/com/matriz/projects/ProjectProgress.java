package com.matriz.projects;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The progress of projects, as Projects shows it (FR-012, FR-024; US-006, US-025). Projects defines
 * this interface and Progress implements it, so that Projects does not depend on Progress
 * (architecture, module rule 2; ADR-008). Called inside the transaction of the use case that asks.
 */
public interface ProjectProgress {

	/**
	 * The progress of each of the projects, in one query.
	 * @return one entry for each of the projects
	 */
	Map<ProjectId, Figures> of(Collection<ProjectId> projects);

	/** The progress of one project. */
	default Figures of(ProjectId project) {
		return of(List.of(project)).get(project);
	}

	/**
	 * A project's task counts by state, not counting deleted tasks, and its percentage Done.
	 * @param percentDone the rounded percentage Done (Assumption A-026), or {@code null} when the
	 * project has no tasks (EX-005)
	 */
	record Figures(long toDo, long inProgress, long done, Integer percentDone) {

	}

}
