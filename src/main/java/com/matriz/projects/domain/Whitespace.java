package com.matriz.projects.domain;

/**
 * What counts as whitespace in the texts of a project (Assumption A-031, as amended on 2026-11-05):
 * Java whitespace and the Unicode space separators, so the no-break space (U+00A0) is whitespace too.
 * {@link String#strip()} and {@link String#isBlank()} leave the no-break spaces, which a person cannot
 * see, so they are not used for these texts.
 */
public final class Whitespace {

	private Whitespace() {
	}

	/** The text without whitespace at either end. */
	public static String trim(String text) {
		int start = 0;
		int end = text.length();
		while (start < end && isWhitespace(text.charAt(start))) {
			start++;
		}
		while (end > start && isWhitespace(text.charAt(end - 1))) {
			end--;
		}
		return text.substring(start, end);
	}

	/** Whether the text is empty or has only whitespace. */
	public static boolean isBlank(String text) {
		return trim(text).isEmpty();
	}

	private static boolean isWhitespace(char character) {
		return Character.isWhitespace(character) || Character.getType(character) == Character.SPACE_SEPARATOR;
	}

}
