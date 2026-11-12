package com.matriz;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** Shared HTTP pagination parsing; callers retain their access-check order (ADR-004, A-032). */
public final class PageParameters {

	private PageParameters() {
	}

	public static int page(String text) {
		return integer("page", text, 1, Integer.MAX_VALUE, "an integer of at least 1");
	}

	public static int size(String text) {
		return integer("size", text, 20, 100, "an integer from 1 to 100");
	}

	private static int integer(String name, String text, int defaultValue, int maximum, String expected) {
		if (text == null) {
			return defaultValue;
		}
		try {
			int value = Integer.parseInt(text);
			if (value >= 1 && value <= maximum) {
				return value;
			}
		}
		catch (NumberFormatException ex) {
			// The same refusal applies to malformed and out-of-range numbers.
		}
		throw new ErrorResponseException(HttpStatus.BAD_REQUEST,
				ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
						"The query parameter " + name + " must be " + expected + "."), null);
	}

}
