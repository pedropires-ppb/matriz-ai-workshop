package com.matriz;

import java.util.ArrayList;
import java.util.List;

/**
 * Stops start-up when environment variables are missing, blank, or not allowed (ADR-007 decision 6;
 * ADR-005). It carries the names of the variables only, never their values.
 */
public class EnvironmentVariablesException extends RuntimeException {

	private final List<String> missing;

	private final List<String> invalid;

	private final List<String> invalidKeys;

	private final List<String> invalidPasswords;

	public EnvironmentVariablesException(List<String> missing, List<String> invalid, List<String> invalidKeys,
			List<String> invalidPasswords) {
		super(describe(missing, invalid, invalidKeys, invalidPasswords));
		this.missing = List.copyOf(missing);
		this.invalid = List.copyOf(invalid);
		this.invalidKeys = List.copyOf(invalidKeys);
		this.invalidPasswords = List.copyOf(invalidPasswords);
	}

	/** Names of required variables that are missing or blank. */
	public List<String> missing() {
		return this.missing;
	}

	/** Names of variables whose value is not true or false. */
	public List<String> invalid() {
		return this.invalid;
	}

	/** Names of variables whose value is not a Base64-encoded key of at least 256 bits. */
	public List<String> invalidKeys() {
		return this.invalidKeys;
	}

	/** Names of password variables whose value breaks the password rules. */
	public List<String> invalidPasswords() {
		return this.invalidPasswords;
	}

	private static String describe(List<String> missing, List<String> invalid, List<String> invalidKeys,
			List<String> invalidPasswords) {
		List<String> parts = new ArrayList<>();
		if (!missing.isEmpty()) {
			parts.add("These environment variables are missing or blank: " + String.join(", ", missing) + ".");
		}
		if (!invalid.isEmpty()) {
			parts.add("These environment variables must be true or false: " + String.join(", ", invalid) + ".");
		}
		if (!invalidKeys.isEmpty()) {
			parts.add("These environment variables must be a Base64-encoded key of at least "
					+ EnvironmentCheck.SIGNING_KEY_MINIMUM_BYTES
					+ " bytes, such as the output of openssl rand -base64 32: " + String.join(", ", invalidKeys) + ".");
		}
		if (!invalidPasswords.isEmpty()) {
			parts.add("These environment variables must have at least " + EnvironmentCheck.PASSWORD_MINIMUM_CHARACTERS
					+ " characters and at most " + EnvironmentCheck.PASSWORD_MAXIMUM_BYTES + " bytes in UTF-8: "
					+ String.join(", ", invalidPasswords) + ".");
		}
		return String.join(" ", parts);
	}

}
