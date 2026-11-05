package com.matriz.tasks.application;

import java.util.List;

import com.matriz.identity.PersonId;
import com.matriz.projects.ProjectId;
import com.matriz.tasks.TaskId;
import com.matriz.tasks.TaskPriority;
import com.matriz.tasks.TaskState;

/**
 * A task as shown to a member (openapi.yaml, Task).
 * @param description {@code null} if the task has none
 * @param assignees ordered by display name, then by identifier
 */
public record TaskDetails(TaskId taskId, ProjectId projectId, String title, String description, TaskState state,
		TaskPriority priority, List<Assignee> assignees) {

	/** A person assigned to a task (openapi.yaml, PersonSummary). */
	public record Assignee(PersonId personId, String displayName) {

	}

}
