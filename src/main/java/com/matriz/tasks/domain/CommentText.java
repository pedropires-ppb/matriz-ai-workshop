package com.matriz.tasks.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A comment's text: 1 to 2000 characters after trimming, counted as Unicode characters
 * (Assumption A-036). Whitespace is that of Assumption A-033, so a text of only no-break spaces is
 * empty. The trimmed text is what is kept.
 */
@Embeddable
public record CommentText(@Column(name = "text") String value) {

	public static final int MAXIMUM_LENGTH = 2000;

	public static final String REQUIRED = "The text is required.";

	public static final String TOO_LONG = "The text must have at most " + MAXIMUM_LENGTH + " characters.";

	public static final String CANNOT_BE_STORED = "The text contains a character that cannot be stored.";

	/**
	 * @throws IllegalArgumentException with the text to show, if the text is empty after trimming, too
	 * long, or has a character that cannot be stored (Assumption A-037)
	 */
	public CommentText {
		if (value == null) {
			throw new IllegalArgumentException(REQUIRED);
		}
		if (!StorableText.isStorable(value)) {
			throw new IllegalArgumentException(CANNOT_BE_STORED);
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
