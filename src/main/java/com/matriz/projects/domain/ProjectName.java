package com.matriz.projects.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A project's name: 1 to 100 characters after trimming, counted as Unicode characters
 * (Assumption A-003 and A-031). Names are not unique (OQ-003). The text is trimmed here, so a
 * stored name is never padded.
 */
@Embeddable
public record ProjectName(@Column(name = "name") String value) {

	public static final int MAXIMUM_LENGTH = 100;

	public static final String REQUIRED = "A name is required.";

	public static final String TOO_LONG = "The name must have at most " + MAXIMUM_LENGTH + " characters.";

	/** @throws IllegalArgumentException with the text to show, if the name is empty or too long */
	public ProjectName {
		if (value == null) {
			throw new IllegalArgumentException(REQUIRED);
		}
		value = value.strip();
		if (value.isEmpty()) {
			throw new IllegalArgumentException(REQUIRED);
		}
		if (value.codePointCount(0, value.length()) > MAXIMUM_LENGTH) {
			throw new IllegalArgumentException(TOO_LONG);
		}
	}

}
