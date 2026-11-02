package com.matriz.identity.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The hash of a person's password, made by an adaptive password-hashing function (ADR-005
 * decision 5). It is never logged or returned, so {@link #toString()} does not show it.
 */
@Embeddable
public record PasswordHash(@Column(name = "password_hash") String value) {

	public PasswordHash {
		Objects.requireNonNull(value, "A password hash is required.");
		if (value.isEmpty()) {
			throw new IllegalArgumentException("A password hash is required.");
		}
	}

	@Override
	public String toString() {
		return "PasswordHash[hidden]";
	}

}
