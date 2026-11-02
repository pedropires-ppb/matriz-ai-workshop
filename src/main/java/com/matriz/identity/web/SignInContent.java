package com.matriz.identity.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.node.NullNode;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * The content of a sign-in request, checked against {@code SignInRequest} of openapi.yaml. It holds
 * a password, so {@link #toString()} shows nothing of it, even in debug logs.
 */
@JsonDeserialize(using = SignInContent.Reader.class)
final class SignInContent {

	static final String LOGIN_NAME = "loginName";

	static final String PASSWORD = "password";

	static final String REQUIRED = "A login name and a password are required.";

	static final String ONLY_LOGIN_NAME_AND_PASSWORD = "Only a login name and a password can be sent.";

	private final JsonNode content;

	private SignInContent(JsonNode content) {
		this.content = content;
	}

	/**
	 * The login name and password, or {@code 422} with each invalid member if the content is not an
	 * object, if {@code loginName} or {@code password} is missing, empty, or not a string, or if
	 * another property is present (openapi.yaml, {@code signIn}).
	 */
	Credentials credentials() {
		if (!this.content.isObject()) {
			throw invalidContent(REQUIRED, List.of(new InvalidMember("", "The content must be a JSON object.")));
		}
		List<InvalidMember> invalid = new ArrayList<>();
		String loginName = nonemptyString(LOGIN_NAME);
		if (loginName == null) {
			invalid.add(new InvalidMember(pointer(LOGIN_NAME), "A login name is required."));
		}
		String password = nonemptyString(PASSWORD);
		if (password == null) {
			invalid.add(new InvalidMember(pointer(PASSWORD), "A password is required."));
		}
		boolean missing = !invalid.isEmpty();
		for (String name : this.content.propertyNames()) {
			if (!LOGIN_NAME.equals(name) && !PASSWORD.equals(name)) {
				invalid.add(new InvalidMember(pointer(name), "This property is not allowed."));
			}
		}
		if (!invalid.isEmpty()) {
			throw invalidContent(missing ? REQUIRED : ONLY_LOGIN_NAME_AND_PASSWORD, invalid);
		}
		return new Credentials(loginName, password);
	}

	private String nonemptyString(String name) {
		JsonNode member = this.content.get(name);
		if (member == null || !member.isString() || member.stringValue().isEmpty()) {
			return null;
		}
		return member.stringValue();
	}

	/** A JSON Pointer (RFC 6901) to a member of the content. */
	private static String pointer(String name) {
		return "/" + name.replace("~", "~0").replace("/", "~1");
	}

	private static ErrorResponseException invalidContent(String detail, List<InvalidMember> errors) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, detail);
		problem.setProperty("errors", List.copyOf(errors));
		return new ErrorResponseException(HttpStatus.UNPROCESSABLE_CONTENT, problem, null);
	}

	@Override
	public String toString() {
		return "SignInContent[hidden]";
	}

	/** The checked login name and password. */
	record Credentials(String loginName, String password) {

		Credentials {
			Objects.requireNonNull(loginName, "A login name is required.");
			Objects.requireNonNull(password, "A password is required.");
		}

		@Override
		public String toString() {
			return "Credentials[hidden]";
		}

	}

	/** A member of the content that does not satisfy the request schema (openapi.yaml, ValidationProblem). */
	record InvalidMember(String pointer, String detail) {

	}

	/**
	 * Reads any JSON value, including {@code null}, so that every well-formed content that is not a
	 * valid object receives {@code 422}, not {@code 400}.
	 */
	static final class Reader extends ValueDeserializer<SignInContent> {

		@Override
		public SignInContent deserialize(JsonParser parser, DeserializationContext context) {
			return new SignInContent(context.readTree(parser));
		}

		@Override
		public SignInContent getNullValue(DeserializationContext context) {
			return new SignInContent(NullNode.getInstance());
		}

	}

}
