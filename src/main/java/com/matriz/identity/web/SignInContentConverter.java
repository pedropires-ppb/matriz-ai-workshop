package com.matriz.identity.web;

import java.io.IOException;
import java.util.List;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Reads the content of a sign-in request as the application's JSON converter does, but never
 * repeats that content in an error. For content that is not well-formed JSON, Jackson's message
 * quotes the offending text, for example a password sent without quotes, and Spring MVC writes that
 * message to its debug log. This converter replaces the message with one that quotes nothing; the
 * response stays the same {@code 400}, whose detail is in messages.properties (Lab Worksheet 03,
 * decision on logging: no log contains a password).
 */
final class SignInContentConverter implements HttpMessageConverter<SignInContent> {

	static final String NOT_WELL_FORMED = "The sign-in content is not well-formed JSON; its text is not repeated.";

	private final HttpMessageConverter<Object> json;

	SignInContentConverter(JsonMapper jsonMapper) {
		this.json = new JacksonJsonHttpMessageConverter(jsonMapper);
	}

	@Override
	public boolean canRead(Class<?> type, MediaType mediaType) {
		return SignInContent.class.equals(type) && this.json.canRead(type, mediaType);
	}

	@Override
	public boolean canWrite(Class<?> type, MediaType mediaType) {
		return false;
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return this.json.getSupportedMediaTypes();
	}

	@Override
	public SignInContent read(Class<? extends SignInContent> type, HttpInputMessage input) throws IOException {
		try {
			return (SignInContent) this.json.read(type, input);
		}
		catch (HttpMessageNotReadableException ex) {
			// Neither the message nor the cause of ex is kept: both can quote the content.
			throw new HttpMessageNotReadableException(NOT_WELL_FORMED, input);
		}
	}

	@Override
	public void write(SignInContent content, MediaType contentType, HttpOutputMessage output) {
		throw new UnsupportedOperationException("The content of a sign-in request is only read.");
	}

	/** Places the converter before the application's JSON converter. */
	@Configuration(proxyBeanMethods = false)
	static class Registration implements WebMvcConfigurer {

		private final JsonMapper jsonMapper;

		Registration(JsonMapper jsonMapper) {
			this.jsonMapper = jsonMapper;
		}

		@Override
		public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
			builder.addCustomConverter(new SignInContentConverter(this.jsonMapper));
		}

	}

}
