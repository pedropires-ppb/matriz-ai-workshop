package com.matriz.tasks.application;

/** The project is Completed, so its tasks cannot be changed: 409 (ADR-004). */
public class ProjectCompletedException extends RuntimeException {

	public ProjectCompletedException() {
		super("The project is Completed; it cannot be changed.");
	}

}
