package com.matriz.projects.application;

/** The person is a member, but only the Project Owner may do this: 403 (ADR-004). */
public class NotProjectOwnerException extends RuntimeException {

	public NotProjectOwnerException() {
		super("Only the Project Owner of this project can do this.");
	}

}
