package com.matriz.identity.application;

import com.matriz.identity.PersonId;
import com.matriz.identity.domain.Person;

/** A person as shown to others: identifier and display name only (openapi.yaml, PersonSummary). */
public record PersonDetails(PersonId personId, String displayName) {

	static PersonDetails of(Person person) {
		return new PersonDetails(person.id(), person.displayName().value());
	}

}
