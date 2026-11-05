package com.matriz.projects.application;

/** The project does not exist, is deleted, or the person is not a member: 404 (ADR-004). */
public class ProjectNotFoundException extends RuntimeException {

	public ProjectNotFoundException() {
		super("The project does not exist or is not accessible.");
	}

}
