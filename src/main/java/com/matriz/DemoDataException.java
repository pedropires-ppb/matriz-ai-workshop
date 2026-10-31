package com.matriz;

/**
 * Stops start-up when the demonstration data setting does not match the database, or when the
 * demonstration data cannot be added (ADR-007 decision 10). Its message contains no password and no
 * password hash.
 */
class DemoDataException extends RuntimeException {

	private final String action;

	DemoDataException(String message, String action) {
		super(message);
		this.action = action;
	}

	/** What the person starting Matriz can do about it. */
	String action() {
		return this.action;
	}

}
