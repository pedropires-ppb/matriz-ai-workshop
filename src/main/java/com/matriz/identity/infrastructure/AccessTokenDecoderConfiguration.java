package com.matriz.identity.infrastructure;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.matriz.identity.domain.EndedAccessTokens;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Verifies the access token of every request that carries one: its HS256 signature, and then
 * {@link AccessTokenValidator}. No other algorithm is accepted (ADR-005, open point "Signing").
 */
@Configuration(proxyBeanMethods = false)
class AccessTokenDecoderConfiguration {

	private static final Converter<Map<String, Object>, Map<String, Object>> STANDARD_CLAIMS = MappedJwtClaimSetConverter
		.withDefaults(Collections.emptyMap());

	@Bean
	JwtDecoder accessTokenDecoder(AccessTokenSigningKey signingKey, EndedAccessTokens endedTokens) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(signingKey.key())
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		decoder.setClaimSetConverter(AccessTokenDecoderConfiguration::claims);
		decoder.setJwtValidator(new AccessTokenValidator(endedTokens));
		return decoder;
	}

	/**
	 * The token's claims, converted as usual, except that a missing {@code iat} stays missing, so
	 * that {@link AccessTokenValidator} refuses the token. By default, it would be set to one second
	 * before {@code exp}.
	 */
	static Map<String, Object> claims(Map<String, Object> claims) {
		Map<String, Object> converted = STANDARD_CLAIMS.convert(claims);
		if (claims.containsKey(JwtClaimNames.IAT)) {
			return converted;
		}
		Map<String, Object> withoutIssuedAt = new HashMap<>(converted);
		withoutIssuedAt.remove(JwtClaimNames.IAT);
		return withoutIssuedAt;
	}

}
