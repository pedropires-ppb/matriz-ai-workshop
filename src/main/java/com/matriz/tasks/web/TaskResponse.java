package com.matriz.tasks.web;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.matriz.tasks.application.TaskDetails;
import com.matriz.tasks.application.TasksPage;

/**
 * A task (openapi.yaml, Task). {@code description} is left out when the task has none.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TaskResponse(String taskId, String projectId, String title, String description, String state,
		String priority, List<Assignee> assignees) {

	static TaskResponse of(TaskDetails task) {
		return new TaskResponse(task.taskId().toString(), task.projectId().toString(), task.title(),
				task.description(), task.state().name(), task.priority().name(),
				task.assignees().stream().map(Assignee::of).toList());
	}

	/** A person assigned to a task (openapi.yaml, PersonSummary). */
	record Assignee(String personId, String displayName) {

		static Assignee of(TaskDetails.Assignee assignee) {
			return new Assignee(assignee.personId().toString(), assignee.displayName());
		}

	}

	/** One page of the tasks of a project (openapi.yaml, TaskPage). */
	record Page(List<TaskResponse> items, int page, int size, long totalItems, long totalPages) {

		static Page of(TasksPage page) {
			return new Page(page.items().stream().map(TaskResponse::of).toList(), page.page(), page.size(),
					page.totalItems(), page.totalPages());
		}

	}

}
