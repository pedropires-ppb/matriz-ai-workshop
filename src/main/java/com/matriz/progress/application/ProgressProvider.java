package com.matriz.progress.application;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import com.matriz.progress.domain.Progress;
import com.matriz.projects.ProjectId;
import com.matriz.projects.ProjectProgress;
import com.matriz.tasks.TaskCounting;
import com.matriz.tasks.TaskCounting.TaskCounts;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gives Projects the progress of its projects, from the counts of tasks by state that Tasks provides
 * (ADR-008, layers of Progress). It joins the transaction of the use case that calls it.
 */
@Component
class ProgressProvider implements ProjectProgress {

	private final TaskCounting tasks;

	ProgressProvider(TaskCounting tasks) {
		this.tasks = tasks;
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
	public Map<ProjectId, Figures> of(Collection<ProjectId> projects) {
		Map<ProjectId, Figures> figures = new HashMap<>();
		this.tasks.countByState(projects).forEach((project, counts) -> figures.put(project, figures(counts)));
		return figures;
	}

	private static Figures figures(TaskCounts counts) {
		Progress progress = new Progress(counts.toDo(), counts.inProgress(), counts.done());
		OptionalInt percent = progress.percentDone();
		return new Figures(progress.toDo(), progress.inProgress(), progress.done(),
				percent.isPresent() ? percent.getAsInt() : null);
	}

}
