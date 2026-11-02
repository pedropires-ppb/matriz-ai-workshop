package com.matriz.identity.infrastructure;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The key that signs and verifies the access tokens with HS256 (ADR-005, open point "Signing").
 * It comes only from the environment variable {@code MATRIZ_JWT_SIGNING_KEY}, which start-up has
 * already checked; it is never logged. Replacing it means restarting, which signs everyone out.
 */
@Component
class AccessTokenSigningKey {

	static final String ALGORITHM = "HmacSHA256";

	private final SecretKey key;

	AccessTokenSigningKey(@Value("${MATRIZ_JWT_SIGNING_KEY}") String encodedKey) {
		this.key = new SecretKeySpec(Base64.getDecoder().decode(encodedKey), ALGORITHM);
	}

	SecretKey key() {
		return this.key;
	}

	@Override
	public String toString() {
		return "AccessTokenSigningKey[hidden]";
	}

}
