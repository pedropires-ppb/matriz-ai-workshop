package com.matriz.identity.application;

import java.time.Duration;
import java.util.Objects;

/**
 * The result of signing in: the access token, how long it lasts, and the signed-in person. The
 * token is never logged, so {@link #toString()} does not show it.
 */
public record SignedIn(String accessToken, Duration lifetime, PersonDetails person) {

	public SignedIn {
		Objects.requireNonNull(accessToken, "A token is required.");
		Objects.requireNonNull(lifetime, "A lifetime is required.");
		Objects.requireNonNull(person, "A person is required.");
	}

	@Override
	public String toString() {
		return "SignedIn[accessToken=hidden, lifetime=" + this.lifetime + ", person=" + this.person + "]";
	}

}
