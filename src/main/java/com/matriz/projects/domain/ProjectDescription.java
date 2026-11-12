package com.matriz.projects.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A project's description: at most 1000 characters after trimming, counted as Unicode characters
 * (Assumption A-031). It is optional: a project without one holds {@code null}, never an empty
 * description, so use {@link #ofNullable(String)} for text that may be empty.
 */
@Embeddable
public record ProjectDescription(@Column(name = "description") String value) {

	public static final int MAXIMUM_LENGTH = 1000;

	public static final String TOO_LONG = "The description must have at most " + MAXIMUM_LENGTH + " characters.";

	public static final String CANNOT_BE_STORED = "The description contains a character that cannot be stored.";

	/**
	 * @throws IllegalArgumentException with the text to show, if the description is empty, too long, or
	 * has a character that cannot be stored (Assumption A-037)
	 */
	public ProjectDescription {
		if (value == null) {
			throw new IllegalArgumentException("A description cannot be absent; use no description instead.");
		}
		if (!StorableText.isStorable(value)) {
			throw new IllegalArgumentException(CANNOT_BE_STORED);
		}
		value = Whitespace.trim(value);
		if (value.isEmpty()) {
			throw new IllegalArgumentException("A description cannot be empty; use no description instead.");
		}
		if (value.codePointCount(0, value.length()) > MAXIMUM_LENGTH) {
			throw new IllegalArgumentException(TOO_LONG);
		}
	}

	/**
	 * The description, or {@code null}, meaning no description, if the text is {@code null} or empty
	 * after trimming.
	 * @throws IllegalArgumentException with the text to show, if the text is too long
	 */
	public static ProjectDescription ofNullable(String text) {
		if (text == null || Whitespace.isBlank(text)) {
			return null;
		}
		return new ProjectDescription(text);
	}

}
