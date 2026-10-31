package com.matriz;

import java.util.ArrayList;
import java.util.List;

/**
 * Stops start-up when environment variables are missing, blank, or not allowed (ADR-007 decision 6).
 * It carries the names of the variables only, never their values.
 */
public class EnvironmentVariablesException extends RuntimeException {

	private final List<String> missing;

	private final List<String> invalid;

	public EnvironmentVariablesException(List<String> missing, List<String> invalid) {
		super(describe(missing, invalid));
		this.missing = List.copyOf(missing);
		this.invalid = List.copyOf(invalid);
	}

	/** Names of required variables that are missing or blank. */
	public List<String> missing() {
		return this.missing;
	}

	/** Names of variables whose value is not one of the allowed values. */
	public List<String> invalid() {
		return this.invalid;
	}

	private static String describe(List<String> missing, List<String> invalid) {
		List<String> parts = new ArrayList<>();
		if (!missing.isEmpty()) {
			parts.add("These environment variables are missing or blank: " + String.join(", ", missing) + ".");
		}
		if (!invalid.isEmpty()) {
			parts.add("These environment variables must be true or false: " + String.join(", ", invalid) + ".");
		}
		return String.join(" ", parts);
	}

}
