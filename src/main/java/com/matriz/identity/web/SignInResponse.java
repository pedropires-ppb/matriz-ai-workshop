package com.matriz.identity.web;

import com.matriz.identity.application.SignedIn;

/**
 * The result of signing in (openapi.yaml, {@code SignInResponse}). It holds the access token, so
 * {@link #toString()} does not show it, even in debug logs.
 */
record SignInResponse(String accessToken, String tokenType, long expiresIn, PersonSummary person) {

	static final String BEARER = "Bearer";

	static SignInResponse of(SignedIn signedIn) {
		return new SignInResponse(signedIn.accessToken(), BEARER, signedIn.lifetime().toSeconds(),
				PersonSummary.of(signedIn.person()));
	}

	@Override
	public String toString() {
		return "SignInResponse[accessToken=hidden, tokenType=" + this.tokenType + ", expiresIn=" + this.expiresIn
				+ ", person=" + this.person + "]";
	}

}
