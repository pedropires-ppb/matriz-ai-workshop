package com.matriz.projects.application;

/**
 * The project does not exist, is deleted, or the person is not a member, or the person to remove is
 * not a member of it: 404 (ADR-004; openapi.yaml, removeParticipant).
 */
public class MemberNotFoundException extends RuntimeException {

	public MemberNotFoundException() {
		super("The project or member does not exist or is not accessible.");
	}

}
