package com.matriz.identity.web;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Answers a request whose identity is not established with the same Problem Details as every
 * other error (ADR-004 decision 3): it hands the error to Spring MVC's exception resolvers, as if a
 * controller had thrown it.
 * <ul>
 * <li>A refused token, including a malformed {@code Authorization} header, gets {@code 401} and
 * {@code Bearer error="invalid_token"}, with no description.</li>
 * <li>A token that could not be checked, for example because the database is unavailable, gets
 * {@code 500}, which is logged; the token is not accepted.</li>
 * <li>A request with no token gets {@code 401} and {@code Bearer}.</li>
 * </ul>
 */
final class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final HandlerExceptionResolver exceptionResolver;

	ProblemDetailsAuthenticationEntryPoint(HandlerExceptionResolver exceptionResolver) {
		this.exceptionResolver = exceptionResolver;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
			throws IOException {
		Exception problem;
		if (ex instanceof OAuth2AuthenticationException) {
			problem = AccessProblems.invalidToken();
		}
		else if (ex instanceof AuthenticationServiceException) {
			problem = ex;
		}
		else {
			problem = AccessProblems.noToken();
		}
		if (this.exceptionResolver.resolveException(request, response, null, problem) == null) {
			response.sendError((problem == ex) ? HttpStatus.INTERNAL_SERVER_ERROR.value()
					: HttpStatus.UNAUTHORIZED.value());
		}
	}

}
