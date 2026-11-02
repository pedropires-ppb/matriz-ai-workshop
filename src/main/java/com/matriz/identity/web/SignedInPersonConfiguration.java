package com.matriz.identity.web;

import java.util.List;
import java.util.UUID;

import com.matriz.identity.PersonId;
import com.matriz.identity.SignedInPerson;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Gives every controller method that has a {@link SignedInPerson} parameter the person in the
 * verified access token (ADR-009 decision 3). Other modules thus learn who is acting without using
 * Spring Security.
 */
@Configuration(proxyBeanMethods = false)
class SignedInPersonConfiguration implements WebMvcConfigurer {

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new SignedInPersonResolver());
	}

	static final class SignedInPersonResolver implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return SignedInPerson.class.equals(parameter.getParameterType());
		}

		@Override
		public SignedInPerson resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
			Authentication authentication = SecurityContextHolder.getContextHolderStrategy()
				.getContext()
				.getAuthentication();
			if (authentication instanceof JwtAuthenticationToken token) {
				return new SignedInPerson(new PersonId(UUID.fromString(token.getToken().getSubject())));
			}
			throw AccessProblems.noToken();
		}

	}

}
