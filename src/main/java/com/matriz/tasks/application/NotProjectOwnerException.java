package com.matriz.tasks.application;

/**
 * The person is a member, but is not allowed to do this: only the Project Owner, or, for some
 * actions, an assignee or the person themselves may: 403 (ADR-004).
 */
public class NotProjectOwnerException extends RuntimeException {

	public NotProjectOwnerException() {
		this("Only the Project Owner of this project can do this.");
	}

	public NotProjectOwnerException(String message) {
		super(message);
	}

}
