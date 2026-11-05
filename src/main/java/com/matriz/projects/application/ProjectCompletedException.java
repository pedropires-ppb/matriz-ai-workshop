package com.matriz.projects.application;

/** The project is Completed, so it cannot be changed: 409 (ADR-004). */
public class ProjectCompletedException extends RuntimeException {

	public ProjectCompletedException() {
		super("The project is Completed; it cannot be changed.");
	}

}
