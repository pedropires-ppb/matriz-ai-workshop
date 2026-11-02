package com.matriz.identity.application;

import java.util.Optional;

import com.matriz.identity.PersonId;
import com.matriz.identity.domain.PersonRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shows who is signed in: the person in the access token (FR-015; ADR-005 decision 4). */
@Service
public class GetCurrentPerson {

	private final PersonRepository people;

	GetCurrentPerson(PersonRepository people) {
		this.people = people;
	}

	/** The person, or nothing if no person has this identifier any more. */
	@Transactional(readOnly = true)
	public Optional<PersonDetails> getCurrentPerson(PersonId personId) {
		return this.people.findById(personId).map(PersonDetails::of);
	}

}
