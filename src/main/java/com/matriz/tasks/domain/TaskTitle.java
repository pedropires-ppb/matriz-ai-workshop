package com.matriz.tasks.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A task's title: 1 to 150 characters after trimming, counted as Unicode characters (Assumption
 * A-004 and A-033). Titles are not unique (OQ-003). The text is trimmed here, so a stored title is
 * never padded.
 */
@Embeddable
public record TaskTitle(@Column(name = "title") String value) {

	public static final int MAXIMUM_LENGTH = 150;

	public static final String REQUIRED = "A title is required.";

	public static final String TOO_LONG = "The title must have at most " + MAXIMUM_LENGTH + " characters.";

	/** @throws IllegalArgumentException with the text to show, if the title is empty or too long */
	public TaskTitle {
		if (value == null) {
			throw new IllegalArgumentException(REQUIRED);
		}
		value = Whitespace.trim(value);
		if (value.isEmpty()) {
			throw new IllegalArgumentException(REQUIRED);
		}
		if (value.codePointCount(0, value.length()) > MAXIMUM_LENGTH) {
			throw new IllegalArgumentException(TOO_LONG);
		}
	}

}
