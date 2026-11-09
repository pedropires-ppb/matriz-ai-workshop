package com.matriz.tasks.domain;

import java.util.List;

import com.matriz.tasks.TaskId;

/** The comments (ADR-008 decision 4). A comment is added and read, never changed or removed (rule 7). */
public interface CommentRepository {

	Comment save(Comment comment);

	/**
	 * Every comment of the task, oldest first: by the date and time it was added, then by the order in
	 * which it was stored (Assumption A-036). The task is checked before, so a deleted task is not asked for.
	 */
	List<Comment> findByTask(TaskId task);

}
