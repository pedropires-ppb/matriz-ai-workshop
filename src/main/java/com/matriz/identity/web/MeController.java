package com.matriz.identity.web;

import com.matriz.identity.SignedInPerson;
import com.matriz.identity.application.GetCurrentPerson;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in person (openapi.yaml, {@code getCurrentPerson}; FR-015). The person comes only from
 * the access token: query parameters and other headers are ignored (ADR-009 decision 2). A token
 * whose person no longer exists is refused like any other invalid token.
 */
@RestController
class MeController {

	private final GetCurrentPerson getCurrentPerson;

	MeController(GetCurrentPerson getCurrentPerson) {
		this.getCurrentPerson = getCurrentPerson;
	}

	@GetMapping("/api/me")
	PersonSummary getCurrentPerson(SignedInPerson person) {
		return this.getCurrentPerson.getCurrentPerson(person.personId())
			.map(PersonSummary::of)
			.orElseThrow(AccessProblems::invalidToken);
	}

}
