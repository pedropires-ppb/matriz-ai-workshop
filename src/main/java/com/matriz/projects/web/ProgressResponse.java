package com.matriz.projects.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.matriz.projects.ProjectProgress.Figures;

/**
 * A project's progress (openapi.yaml, Progress). {@code percentDone} is always written, as
 * {@code null} when the project has no tasks (EX-005).
 */
record ProgressResponse(TaskCountsResponse taskCounts,
		@JsonInclude(JsonInclude.Include.ALWAYS) Integer percentDone) {

	static ProgressResponse of(Figures progress) {
		return new ProgressResponse(TaskCountsResponse.of(progress), progress.percentDone());
	}

	/** A project's number of tasks in each state (openapi.yaml, TaskCounts). */
	record TaskCountsResponse(long toDo, long inProgress, long done) {

		static TaskCountsResponse of(Figures progress) {
			return new TaskCountsResponse(progress.toDo(), progress.inProgress(), progress.done());
		}

	}

}
