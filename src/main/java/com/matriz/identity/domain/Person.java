package com.matriz.identity.domain;

import java.util.Objects;

import com.matriz.identity.PersonId;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A person known to Matriz, with a prepared account (glossary; Assumption A-016). The aggregate of
 * the Identity module (ADR-008 decision 2). Matriz has no registration, so people are added only
 * by the demonstration data (ADR-007 decision 10).
 */
@Entity
@Table(name = "person")
public class Person {

	@EmbeddedId
	private PersonId id;

	@Embedded
	private LoginName loginName;

	@Embedded
	private DisplayName displayName;

	@Embedded
	private PasswordHash passwordHash;

	public Person(PersonId id, LoginName loginName, DisplayName displayName, PasswordHash passwordHash) {
		this.id = Objects.requireNonNull(id, "A person identifier is required.");
		this.loginName = Objects.requireNonNull(loginName, "A login name is required.");
		this.displayName = Objects.requireNonNull(displayName, "A display name is required.");
		this.passwordHash = Objects.requireNonNull(passwordHash, "A password hash is required.");
	}

	/** For Jakarta Persistence only. */
	protected Person() {
	}

	public PersonId id() {
		return this.id;
	}

	public LoginName loginName() {
		return this.loginName;
	}

	public DisplayName displayName() {
		return this.displayName;
	}

	public PasswordHash passwordHash() {
		return this.passwordHash;
	}

	@Override
	public String toString() {
		return "Person[" + this.id + "]";
	}

}
