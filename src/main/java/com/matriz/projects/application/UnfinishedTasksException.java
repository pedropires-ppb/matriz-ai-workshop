package com.matriz.projects.application;

/** The project has a task that is To do or In progress, so it cannot be completed: 409 (EX-006). */
public class UnfinishedTasksException extends RuntimeException {

	public UnfinishedTasksException() {
		super("The project has tasks that are To do or In progress; it remains Active.");
	}

}
