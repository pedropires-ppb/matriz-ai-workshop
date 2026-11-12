package com.matriz.projects.domain;

/**
 * Whether the database keeps a text exactly as it was sent (Assumption A-037): it cannot hold the
 * null character (U+0000), and it changes half of a surrogate pair into "?". No other character is
 * refused here (OQ-031).
 */
public final class StorableText {

	private StorableText() {
	}

	public static boolean isStorable(String text) {
		for (int index = 0; index < text.length(); index++) {
			char character = text.charAt(index);
			if (character == '\0' || Character.isLowSurrogate(character)) {
				return false;
			}
			if (Character.isHighSurrogate(character)) {
				if (index + 1 == text.length() || !Character.isLowSurrogate(text.charAt(index + 1))) {
					return false;
				}
				index++;
			}
		}
		return true;
	}

}
