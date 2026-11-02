package com.matriz.identity;

import java.util.Objects;

/**
 * The person acting in a request, taken from the verified access token (ADR-009 decision 2). A
 * controller learns who is acting only through a parameter of this type, which Identity's web
 * layer resolves (ADR-009 decision 3). It never comes from the request's content, query
 * parameters, or other headers.
 */
public record SignedInPerson(PersonId personId) {

	public SignedInPerson {
		Objects.requireNonNull(personId, "A person identifier is required.");
	}

}
