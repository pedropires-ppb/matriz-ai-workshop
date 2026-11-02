package com.matriz.identity.domain;

import java.util.Optional;

/**
 * Checks a password against a person's password hash (ADR-005 decision 5; open point "Passwords
 * and failed attempts").
 */
public interface PasswordVerifier {

	/**
	 * Whether the password matches the hash. Without a hash, as for an unknown login name, the
	 * password is still compared with a dummy hash, so that the response time does not reveal whether
	 * the login name exists; the answer is then {@code false}.
	 */
	boolean matches(String password, Optional<PasswordHash> hash);

}
