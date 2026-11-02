package com.matriz.identity.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** The name by which a person is shown to others (glossary). */
@Embeddable
public record DisplayName(@Column(name = "display_name") String value) {

	public DisplayName {
		Objects.requireNonNull(value, "A display name is required.");
		if (value.isEmpty()) {
			throw new IllegalArgumentException("A display name is required.");
		}
	}

}
