package com.matriz.identity.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * The 401 responses of openapi.yaml, as Problem Details with a {@code WWW-Authenticate} challenge
 * (RFC 6750; ADR-005 decision 2). They never say why a token was refused.
 */
final class AccessProblems {

	static final String NO_TOKEN = "Identity has not been established.";

	static final String INVALID_TOKEN = "The access token is invalid, expired, or no longer accepted.";

	static final String INCORRECT_CREDENTIALS = "The login name or password is incorrect.";

	static final String BEARER_CHALLENGE = "Bearer";

	static final String INVALID_TOKEN_CHALLENGE = "Bearer error=\"invalid_token\"";

	private AccessProblems() {
	}

	/** No token was sent with a request that needs one. */
	static ErrorResponseException noToken() {
		return unauthorized(NO_TOKEN, BEARER_CHALLENGE);
	}

	/**
	 * The token is malformed, invalid, expired, or ended by signing out, or its person no longer
	 * exists.
	 */
	static ErrorResponseException invalidToken() {
		return unauthorized(INVALID_TOKEN, INVALID_TOKEN_CHALLENGE);
	}

	/** A wrong password or an unknown login name (EX-014). HTTP requires a challenge with 401. */
	static ErrorResponseException incorrectCredentials() {
		return unauthorized(INCORRECT_CREDENTIALS, BEARER_CHALLENGE);
	}

	private static ErrorResponseException unauthorized(String detail, String challenge) {
		ErrorResponseException problem = new ErrorResponseException(HttpStatus.UNAUTHORIZED,
				ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail), null);
		problem.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, challenge);
		return problem;
	}

}
