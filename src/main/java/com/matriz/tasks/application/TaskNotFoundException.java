package com.matriz.tasks.application;

/**
 * The project or the task does not exist, is deleted, or the person is not a member of the project:
 * 404 (ADR-004). The same answer for every case, so nothing is disclosed (NFR-002).
 */
public class TaskNotFoundException extends RuntimeException {

	private TaskNotFoundException(String message) {
		super(message);
	}

	/** For the operations on the tasks of a project: listing and creating. */
	public static TaskNotFoundException ofProject() {
		return new TaskNotFoundException("The project does not exist or is not accessible.");
	}

	/** For the operations on one task. */
	public static TaskNotFoundException ofTask() {
		return new TaskNotFoundException("The project or task does not exist or is not accessible.");
	}

}
