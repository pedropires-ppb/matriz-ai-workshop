package com.matriz.projects.application;

import java.util.List;

/**
 * The name or the description does not satisfy the rules of Assumption A-031: 422 (ADR-004). Each
 * invalid member is named by its JSON Pointer in the request content.
 */
public class InvalidProjectException extends RuntimeException {

	private final List<InvalidField> fields;

	public InvalidProjectException(List<InvalidField> fields) {
		this("The project is invalid.", fields);
	}

	public InvalidProjectException(String detail, List<InvalidField> fields) {
		super(detail);
		this.fields = List.copyOf(fields);
	}

	public List<InvalidField> fields() {
		return this.fields;
	}

	/** A member of the content that is invalid, and why. */
	public record InvalidField(String pointer, String detail) {

	}

}
