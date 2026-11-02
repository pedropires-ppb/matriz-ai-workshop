package com.matriz.identity.infrastructure;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.matriz.identity.PersonId;
import com.matriz.identity.domain.AccessTokenIssuer;
import com.matriz.identity.domain.IssuedAccessToken;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * Issues the access tokens: JSON Web Tokens signed with HS256 whose header holds exactly
 * {@code alg} and {@code typ}, and whose claims are exactly {@code sub} (the person's identifier),
 * {@code jti} (a random UUID), {@code iat}, and {@code exp} (ADR-005, open points "Token lifetime"
 * and "Signing"). The lifetime is {@code matriz.security.token-lifetime}.
 */
@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {

	static final String TYPE = "JWT";

	private final JwtEncoder encoder;

	private final Duration lifetime;

	JwtAccessTokenIssuer(AccessTokenSigningKey signingKey,
			@Value("${matriz.security.token-lifetime}") Duration lifetime) {
		if (lifetime.isNegative() || lifetime.toSeconds() < 1) {
			throw new IllegalStateException("matriz.security.token-lifetime must be at least one second.");
		}
		// A key without a key ID: NimbusJwtEncoder.withSecretKey would put the key's thumbprint,
		// a hash of the signing key, in every token's header.
		this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(signingKey.key()));
		this.lifetime = lifetime;
	}

	@Override
	public IssuedAccessToken issue(PersonId person) {
		Instant issuedAt = Instant.now();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type(TYPE).build();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(person.value().toString())
			.id(UUID.randomUUID().toString())
			.issuedAt(issuedAt)
			.expiresAt(issuedAt.plus(this.lifetime))
			.build();
		String token = this.encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new IssuedAccessToken(token, this.lifetime);
	}

}
