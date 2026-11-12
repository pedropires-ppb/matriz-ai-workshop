package com.matriz.identity.application;

import java.util.Optional;

import com.matriz.identity.domain.AccessTokenIssuer;
import com.matriz.identity.domain.IssuedAccessToken;
import com.matriz.identity.domain.LoginName;
import com.matriz.identity.domain.PasswordVerifier;
import com.matriz.identity.domain.Person;
import com.matriz.identity.domain.PersonRepository;
import com.matriz.identity.domain.StorableText;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Signs a person in with their login name and password (FR-013; ADR-005 decision 1). A wrong
 * password and an unknown login name give the same answer, and take about the same time (EX-014).
 * No attempt is limited or logged (ADR-005, open point "Passwords and failed attempts").
 */
@Service
public class SignIn {

	private final PersonRepository people;

	private final PasswordVerifier passwords;

	private final AccessTokenIssuer tokens;

	SignIn(PersonRepository people, PasswordVerifier passwords, AccessTokenIssuer tokens) {
		this.people = people;
		this.passwords = passwords;
		this.tokens = tokens;
	}

	/**
	 * Returns the new access token and the signed-in person, or nothing if the login name or the
	 * password is incorrect. The login name may be in any letter case; the password must not be empty.
	 * A login name that the database cannot keep (Assumption A-037) belongs to no one, so it is
	 * incorrect like any unknown login name, and the password is checked all the same.
	 */
	@Transactional(readOnly = true)
	public Optional<SignedIn> signIn(String loginName, String password) {
		Optional<Person> person = StorableText.isStorable(loginName)
				? this.people.findByLoginName(LoginName.entered(loginName)) : Optional.empty();
		boolean matches = this.passwords.matches(password, person.map(Person::passwordHash));
		if (!matches || person.isEmpty()) {
			return Optional.empty();
		}
		IssuedAccessToken token = this.tokens.issue(person.get().id());
		return Optional.of(new SignedIn(token.value(), token.lifetime(), PersonDetails.of(person.get())));
	}

}
