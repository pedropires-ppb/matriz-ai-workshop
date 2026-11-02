package com.matriz.identity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Identifies a person (ADR-008 decision 2). Other modules refer to a person only by this
 * identifier, so it is in Identity's API package (ADR-008, mapping conventions). The column is
 * {@code person_id}; an aggregate whose column has another name overrides it.
 */
@Embeddable
public record PersonId(@Column(name = "person_id") UUID value) implements Serializable {

	public PersonId {
		Objects.requireNonNull(value, "A person identifier is required.");
	}

	@Override
	public String toString() {
		return this.value.toString();
	}

}
