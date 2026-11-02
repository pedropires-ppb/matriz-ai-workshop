package com.matriz.identity.domain;

import java.util.Locale;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The unique name a person enters to sign in (glossary). Login names are case-insensitive, so
 * they are kept in lower case (ADR-007, open point "Case of login names"; migration V3).
 */
@Embeddable
public record LoginName(@Column(name = "login_name") String value) {

	public LoginName {
		Objects.requireNonNull(value, "A login name is required.");
		if (value.isEmpty()) {
			throw new IllegalArgumentException("A login name is required.");
		}
		if (!value.equals(value.toLowerCase(Locale.ROOT))) {
			throw new IllegalArgumentException("A login name is kept in lower case.");
		}
	}

	/** The login name that a person entered, in any letter case. */
	public static LoginName entered(String text) {
		return new LoginName(text.toLowerCase(Locale.ROOT));
	}

}
