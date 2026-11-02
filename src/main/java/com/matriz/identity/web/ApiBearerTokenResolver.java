package com.matriz.identity.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Reads the access token from the {@code Authorization: Bearer} header of API requests only
 * (ADR-005 decision 2). Signing in ignores any {@code Authorization} header (openapi.yaml,
 * {@code signIn}), and a token is never read from the query or the content.
 */
final class ApiBearerTokenResolver implements BearerTokenResolver {

	private final RequestMatcher api = PathPatternRequestMatcher.pathPattern(SecurityConfiguration.API);

	private final RequestMatcher signIn = PathPatternRequestMatcher.pathPattern(HttpMethod.POST,
			SecurityConfiguration.SESSION);

	private final BearerTokenResolver header = new DefaultBearerTokenResolver();

	@Override
	public String resolve(HttpServletRequest request) {
		if (!this.api.matches(request) || this.signIn.matches(request)) {
			return null;
		}
		return this.header.resolve(request);
	}

}
