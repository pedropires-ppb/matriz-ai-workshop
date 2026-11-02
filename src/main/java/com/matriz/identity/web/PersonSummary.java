package com.matriz.identity.web;

import com.matriz.identity.application.PersonDetails;

/**
 * A person as shown to others: identifier and display name only; never the login name or the
 * password hash (openapi.yaml, {@code PersonSummary}).
 */
record PersonSummary(String personId, String displayName) {

	static PersonSummary of(PersonDetails person) {
		return new PersonSummary(person.personId().toString(), person.displayName());
	}

}
