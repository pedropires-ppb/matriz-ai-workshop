package com.matriz.identity.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * A signed access token issued at sign-in, and how long it lasts (ADR-005, open points "Token
 * lifetime" and "Signing"). It is never logged, so {@link #toString()} does not show it.
 */
public record IssuedAccessToken(String value, Duration lifetime) {

	public IssuedAccessToken {
		Objects.requireNonNull(value, "A token is required.");
		Objects.requireNonNull(lifetime, "A lifetime is required.");
	}

	@Override
	public String toString() {
		return "IssuedAccessToken[hidden, lifetime=" + this.lifetime + "]";
	}

}
