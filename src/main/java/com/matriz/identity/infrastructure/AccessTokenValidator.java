package com.matriz.identity.infrastructure;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.matriz.identity.domain.EndedAccessTokens;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtTypeValidator;

/**
 * Accepts an access token whose signature is valid only if (ADR-005, open points "Token lifetime",
 * "How sign-out is enforced", and "Signing"):
 * <ol>
 * <li>its type, if given, is {@code JWT};</li>
 * <li>it has not expired, with no clock tolerance;</li>
 * <li>it has {@code sub} and {@code jti}, both UUIDs, and {@code iat} and {@code exp};</li>
 * <li>it has not been ended by signing out. Only then is the database read.</li>
 * </ol>
 * The checks stop at the first failure.
 */
final class AccessTokenValidator implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2TokenValidatorResult MISSING_CLAIM = failure("A required claim is missing or invalid.");

	private static final OAuth2TokenValidatorResult ENDED = failure("The token has been ended by signing out.");

	private final List<OAuth2TokenValidator<Jwt>> standardChecks = List.of(JwtTypeValidator.jwt(),
			new JwtTimestampValidator(Duration.ZERO));

	private final EndedAccessTokens endedTokens;

	AccessTokenValidator(EndedAccessTokens endedTokens) {
		this.endedTokens = endedTokens;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		for (OAuth2TokenValidator<Jwt> check : this.standardChecks) {
			OAuth2TokenValidatorResult result = check.validate(token);
			if (result.hasErrors()) {
				return result;
			}
		}
		if (!isUuid(token.getSubject()) || !isUuid(token.getId()) || token.getIssuedAt() == null
				|| token.getExpiresAt() == null) {
			return MISSING_CLAIM;
		}
		if (isEnded(UUID.fromString(token.getId()))) {
			return ENDED;
		}
		return OAuth2TokenValidatorResult.success();
	}

	/**
	 * Whether the token was ended. If that cannot be read, the token is neither accepted nor called
	 * invalid: the failure becomes a {@link JwtException}, which Spring Security reports as a
	 * service failure, not as a refused token.
	 */
	private boolean isEnded(UUID tokenId) {
		try {
			return this.endedTokens.isEnded(tokenId);
		}
		catch (RuntimeException ex) {
			throw new JwtException("Whether the access token was ended could not be read.", ex);
		}
	}

	private static boolean isUuid(String text) {
		if (text == null) {
			return false;
		}
		try {
			return UUID.fromString(text).toString().equals(text);
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private static OAuth2TokenValidatorResult failure(String description) {
		return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null));
	}

}
