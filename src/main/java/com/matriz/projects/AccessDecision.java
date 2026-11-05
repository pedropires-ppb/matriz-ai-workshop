package com.matriz.projects;

/**
 * The access policy's decision, which names the HTTP status that the API answers with (ADR-004
 * decisions 5 and 6).
 */
public enum AccessDecision {

	ALLOWED,

	/** The person is not a member, so nothing is disclosed: 404. */
	NOT_FOUND,

	/** A member without permission for the action: 403. */
	FORBIDDEN,

	/** The project's state does not allow the action: 409. */
	CONFLICT

}
