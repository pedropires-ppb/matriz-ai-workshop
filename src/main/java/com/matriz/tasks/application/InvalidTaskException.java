package com.matriz.tasks.application;

import java.util.List;

/**
 * The content does not satisfy the rules of Assumption A-033 or the contract: 422 (ADR-004). Each
 * invalid member is named by its JSON Pointer in the request content.
 */
public class InvalidTaskException extends RuntimeException {

	private final List<InvalidField> fields;

	public InvalidTaskException(List<InvalidField> fields) {
		this("The task is invalid.", fields);
	}

	public InvalidTaskException(String message, List<InvalidField> fields) {
		super(message);
		this.fields = List.copyOf(fields);
	}

	public List<InvalidField> fields() {
		return this.fields;
	}

	/** A member of the content that is invalid, and why. */
	public record InvalidField(String pointer, String detail) {

	}

}
