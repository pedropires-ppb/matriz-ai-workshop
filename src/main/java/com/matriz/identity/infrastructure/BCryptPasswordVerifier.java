package com.matriz.identity.infrastructure;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import com.matriz.identity.domain.PasswordHash;
import com.matriz.identity.domain.PasswordVerifier;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Checks passwords with Spring Security's delegating password encoder, which uses BCrypt (ADR-005
 * decision 5; open point "Passwords and failed attempts").
 * <ul>
 * <li>An unknown login name is compared with a dummy hash, made once with the same encoder, so it
 * takes about as long as a wrong password.</li>
 * <li>BCrypt reads at most 72 bytes. A longer password cannot match a stored one, because the
 * demonstration passwords are at most 72 bytes; it is refused after a comparison with the dummy
 * hash, and never reaches BCrypt, which could fail on it.</li>
 * </ul>
 */
@Component
class BCryptPasswordVerifier implements PasswordVerifier {

	static final int MAXIMUM_BYTES = 72;

	private static final String DUMMY_PASSWORD = "not a password of any person";

	private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	private final String dummyHash = this.encoder.encode(UUID.randomUUID().toString());

	@Override
	public boolean matches(String password, Optional<PasswordHash> hash) {
		if (password.getBytes(StandardCharsets.UTF_8).length > MAXIMUM_BYTES) {
			this.encoder.matches(DUMMY_PASSWORD, this.dummyHash);
			return false;
		}
		if (hash.isEmpty()) {
			this.encoder.matches(password, this.dummyHash);
			return false;
		}
		return this.encoder.matches(password, hash.get().value());
	}

}
