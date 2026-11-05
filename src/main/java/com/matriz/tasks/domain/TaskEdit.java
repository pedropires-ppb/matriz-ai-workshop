package com.matriz.tasks.domain;

import com.matriz.tasks.TaskPriority;

/**
 * A change to a task's title, description, and priority, as a JSON Merge Patch gives it (Assumption
 * A-033).
 * @param title the new title, or {@code null} to leave it unchanged
 * @param changesDescription whether the description is given; if not, it is left unchanged
 * @param description the new description, or {@code null} to remove it; used only if
 * {@code changesDescription}
 * @param priority the new priority, or {@code null} to leave it unchanged
 */
public record TaskEdit(TaskTitle title, boolean changesDescription, TaskDescription description,
		TaskPriority priority) {

}
