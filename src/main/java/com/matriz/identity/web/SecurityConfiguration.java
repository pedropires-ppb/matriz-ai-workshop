package com.matriz.identity.web;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Which requests need an access token (ADR-005 decision 2; open point "Public paths" of ADR-007).
 * <ul>
 * <li>Every path under {@code /api} needs one, including paths that do not exist, except signing
 * in. The token is checked before anything else, so the order of checks starts with 401
 * (ADR-004).</li>
 * <li>Everything else is public: the User interface's files and the health check. No other
 * actuator endpoint is exposed (ADR-007 decision 7).</li>
 * </ul>
 * No session, cookie, or form login is used. Only servlet web applications have a filter chain;
 * the tests that start Matriz without a web server do not need one.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class SecurityConfiguration {

	static final String API = "/api/**";

	static final String SESSION = "/api/session";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder accessTokenDecoder,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
		AuthenticationEntryPoint entryPoint = new ProblemDetailsAuthenticationEntryPoint(exceptionResolver);
		return http.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.requestCache(AbstractHttpConfigurer::disable)
			.sessionManagement((sessions) -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests((requests) -> requests.requestMatchers(HttpMethod.POST, SESSION)
				.permitAll()
				.requestMatchers(API)
				.authenticated()
				.anyRequest()
				.permitAll())
			.oauth2ResourceServer((resourceServer) -> resourceServer.bearerTokenResolver(new ApiBearerTokenResolver())
				.authenticationEntryPoint(entryPoint)
				.jwt((jwt) -> jwt.decoder(accessTokenDecoder))
				.withObjectPostProcessor(failuresTo(entryPoint)))
			.exceptionHandling((exceptions) -> exceptions.authenticationEntryPoint(entryPoint))
			.build();
	}

	/**
	 * Sends every failure to verify a token to the entry point, including one that is not the
	 * token's fault, which Spring Security would otherwise let escape as a raw error.
	 */
	private static ObjectPostProcessor<BearerTokenAuthenticationFilter> failuresTo(AuthenticationEntryPoint entryPoint) {
		return new ObjectPostProcessor<>() {

			@Override
			public <O extends BearerTokenAuthenticationFilter> O postProcess(O filter) {
				AuthenticationEntryPointFailureHandler failures = new AuthenticationEntryPointFailureHandler(entryPoint);
				failures.setRethrowAuthenticationServiceException(false);
				filter.setAuthenticationFailureHandler(failures);
				return filter;
			}

		};
	}

}
