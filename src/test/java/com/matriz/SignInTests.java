package com.matriz;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.matriz.Contract.assertProblem;
import static com.matriz.TestAccess.INCORRECT_CREDENTIALS;
import static com.matriz.TestAccess.SESSION;
import static com.matriz.TestAccess.signIn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Signing in (openapi.yaml, {@code signIn}; US-013; EX-014; ADR-005). The only public operation:
 * its checks come in the order 415, 400, 422, 401.
 */
@AutoConfigureMockMvc
class SignInTests extends DatabaseTestSupport {

	private static final String REQUIRED = "A login name and a password are required.";

	private static final String ONLY_LOGIN_NAME_AND_PASSWORD = "Only a login name and a password can be sent.";

	private static final String NOT_AN_OBJECT = "The content must be a JSON object.";

	private static final String LOGIN_NAME_REQUIRED = "A login name is required.";

	private static final String PASSWORD_REQUIRED = "A password is required.";

	private static final String NOT_ALLOWED = "This property is not allowed.";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcClient jdbc;

	private TestAccess.TestPerson ana;

	@BeforeEach
	void addAna() {
		this.ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
	}

	@Test
	void validCredentialsGiveATokenForEightHoursAndThePerson() throws Exception {
		MvcResult result = signIn(this.mvc, this.ana.loginName(), this.ana.password()).andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(header().stringValues(HttpHeaders.CACHE_CONTROL, "no-store"))
			.andReturn();

		String content = result.getResponse().getContentAsString();
		Contract.assertSatisfies("SignInResponse", content);
		JsonNode response = JsonMapper.shared().readTree(content);
		assertThat(response.propertyNames()).containsExactlyInAnyOrder("accessToken", "tokenType", "expiresIn",
				"person");
		assertThat(response.path("accessToken").stringValue()).isNotBlank();
		assertThat(response.path("tokenType").stringValue()).isEqualTo("Bearer");
		assertThat(response.path("expiresIn").longValue()).isEqualTo(28800);
		assertThat(response.path("person").propertyNames()).containsExactlyInAnyOrder("personId", "displayName");
		assertThat(response.path("person").path("personId").stringValue()).isEqualTo(this.ana.id().toString());
		assertThat(response.path("person").path("displayName").stringValue()).isEqualTo("Ana Silva");
		assertThat(content).doesNotContain(this.ana.password()).doesNotContain(this.ana.loginName());
	}

	@Test
	void theTokenHasOnlyTheAgreedHeaderAndClaims() throws Exception {
		long before = Instant.now().getEpochSecond();
		String token = TestAccess.accessToken(this.mvc, this.ana);
		long after = Instant.now().getEpochSecond();

		JsonNode header = TestAccess.tokenHeader(token);
		assertThat(header.propertyNames()).containsExactlyInAnyOrder("alg", "typ");
		assertThat(header.path("alg").stringValue()).isEqualTo("HS256");
		assertThat(header.path("typ").stringValue()).isEqualTo("JWT");
		JsonNode claims = TestAccess.tokenClaims(token);
		assertThat(claims.propertyNames()).containsExactlyInAnyOrder("sub", "jti", "iat", "exp");
		assertThat(claims.path("sub").stringValue()).isEqualTo(this.ana.id().toString());
		assertThat(UUID.fromString(claims.path("jti").stringValue())).hasToString(claims.path("jti").stringValue());
		assertThat(claims.path("iat").longValue()).isBetween(before, after);
		assertThat(claims.path("exp").longValue() - claims.path("iat").longValue()).isEqualTo(28800);
	}

	@Test
	void eachSignInIssuesANewToken() throws Exception {
		String first = TestAccess.accessToken(this.mvc, this.ana);
		String second = TestAccess.accessToken(this.mvc, this.ana);

		assertThat(first).isNotEqualTo(second);
		assertThat(TestAccess.tokenClaims(first).path("jti")).isNotEqualTo(TestAccess.tokenClaims(second).path("jti"));
	}

	@Test
	void theLoginNameIgnoresLetterCase() throws Exception {
		String mixedCase = this.ana.loginName().toUpperCase(Locale.ROOT).substring(0, 6)
				+ this.ana.loginName().substring(6);

		signIn(this.mvc, mixedCase, this.ana.password()).andExpect(status().isOk());
		signIn(this.mvc, this.ana.loginName().toUpperCase(Locale.ROOT), this.ana.password())
			.andExpect(status().isOk());
	}

	@Test
	void aWrongPasswordAndAnUnknownLoginNameGetIdenticalResponses() throws Exception {
		List<MockHttpServletResponse> responses = new ArrayList<>();
		for (Map<String, String> credentials : List.of(credentials(this.ana.loginName(), "fictitious-wrong-password"),
				credentials(this.ana.loginName(), this.ana.password().toUpperCase(Locale.ROOT)),
				credentials("test-nobody-" + UUID.randomUUID(), this.ana.password()),
				credentials("test-nobody-" + UUID.randomUUID(), "fictitious-wrong-password"),
				credentials(this.ana.loginName(), "x".repeat(73)))) {
			MvcResult result = assertProblem(this.mvc.perform(post(SESSION).contentType(MediaType.APPLICATION_JSON)
				.content(JsonMapper.shared().writeValueAsString(credentials))), 401, "Unauthorized",
					INCORRECT_CREDENTIALS);
			responses.add(result.getResponse());
		}

		MockHttpServletResponse first = responses.getFirst();
		assertThat(first.getHeaders(HttpHeaders.WWW_AUTHENTICATE)).containsExactly("Bearer");
		for (MockHttpServletResponse response : responses) {
			assertThat(response.getContentAsString()).isEqualTo(first.getContentAsString())
				.doesNotContain(this.ana.loginName());
			assertThat(headers(response)).isEqualTo(headers(first));
		}
	}

	@Test
	void aPasswordLongerThanSeventyTwoBytesIsRefusedLikeAWrongOne() throws Exception {
		String longPassword = this.ana.password() + "-" + "é".repeat(30);

		assertProblem(signIn(this.mvc, this.ana.loginName(), longPassword), 401, "Unauthorized",
				INCORRECT_CREDENTIALS);
	}

	@ParameterizedTest
	@ValueSource(strings = { MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE })
	void contentInAnotherMediaTypeIsUnsupported(String mediaType) throws Exception {
		assertProblem(this.mvc.perform(post(SESSION).contentType(mediaType).content(validContent())), 415,
				"Unsupported Media Type", "The request content must be in the operation's media type.");
	}

	@Test
	void contentWithoutAMediaTypeIsUnsupported() throws Exception {
		assertProblem(this.mvc.perform(post(SESSION).content(validContent())), 415, "Unsupported Media Type",
				"The request content must be in the operation's media type.");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "{\"loginName\": ", "{loginName: 'ana'}" })
	void absentContentOrContentThatIsNotWellFormedJsonIsABadRequest(String content) throws Exception {
		assertProblem(this.mvc.perform(post(SESSION).contentType(MediaType.APPLICATION_JSON).content(content)), 400,
				"Bad Request", "The request content is not well-formed JSON.");
	}

	static Stream<Arguments> contentThatIsNotASignInRequest() {
		return Stream.of(Arguments.of("null", REQUIRED, List.of(List.of("", NOT_AN_OBJECT))),
				Arguments.of("[]", REQUIRED, List.of(List.of("", NOT_AN_OBJECT))),
				Arguments.of("\"ana\"", REQUIRED, List.of(List.of("", NOT_AN_OBJECT))),
				Arguments.of("42", REQUIRED, List.of(List.of("", NOT_AN_OBJECT))),
				Arguments.of("{}", REQUIRED,
						List.of(List.of("/loginName", LOGIN_NAME_REQUIRED), List.of("/password", PASSWORD_REQUIRED))),
				Arguments.of("{\"loginName\": \"ana\", \"password\": \"\"}", REQUIRED,
						List.of(List.of("/password", PASSWORD_REQUIRED))),
				Arguments.of("{\"loginName\": \"\", \"password\": \"fictitious-password\"}", REQUIRED,
						List.of(List.of("/loginName", LOGIN_NAME_REQUIRED))),
				Arguments.of("{\"loginName\": 7, \"password\": null}", REQUIRED,
						List.of(List.of("/loginName", LOGIN_NAME_REQUIRED), List.of("/password", PASSWORD_REQUIRED))),
				Arguments.of("{\"loginName\": \"ana\", \"password\": \"fictitious-password\", \"personId\": \""
						+ UUID.randomUUID() + "\"}", ONLY_LOGIN_NAME_AND_PASSWORD,
						List.of(List.of("/personId", NOT_ALLOWED))),
				Arguments.of("{\"loginName\": \"ana\", \"password\": \"fictitious-password\", \"ownerId\": \"x\", "
						+ "\"authorId\": \"y\"}", ONLY_LOGIN_NAME_AND_PASSWORD,
						List.of(List.of("/ownerId", NOT_ALLOWED), List.of("/authorId", NOT_ALLOWED))),
				Arguments.of("{\"password\": \"fictitious-password\", \"a/b~c\": 1}", REQUIRED,
						List.of(List.of("/loginName", LOGIN_NAME_REQUIRED), List.of("/a~1b~0c", NOT_ALLOWED))));
	}

	@ParameterizedTest
	@MethodSource
	void contentThatIsNotASignInRequest(String content, String detail, List<List<String>> errors) throws Exception {
		MvcResult result = assertProblem(
				this.mvc.perform(post(SESSION).contentType(MediaType.APPLICATION_JSON).content(content)), 422,
				"Unprocessable Content", detail);

		String body = result.getResponse().getContentAsString();
		Contract.assertSatisfies("ValidationProblem", body);
		List<List<String>> pointersAndDetails = new ArrayList<>();
		for (JsonNode error : JsonMapper.shared().readTree(body).path("errors").values()) {
			assertThat(error.propertyNames()).containsExactlyInAnyOrder("pointer", "detail");
			pointersAndDetails.add(List.of(error.path("pointer").stringValue(), error.path("detail").stringValue()));
		}
		assertThat(pointersAndDetails).containsExactlyElementsOf(errors);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Bearer not-a-token", "Bearer !!!", "Basic ZmljdGl0aW91czp4" })
	void anAuthorizationHeaderSentWithTheRequestIsIgnored(String authorization) throws Exception {
		this.mvc
			.perform(post(SESSION).contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.content(JsonMapper.shared()
					.writeValueAsString(credentials(this.ana.loginName(), this.ana.password()))))
			.andExpect(status().isOk());
	}

	@Test
	void aSignedOutTokenSentWithTheRequestIsIgnoredToo() throws Exception {
		String signedOut = TestAccess.signOut(this.mvc, TestAccess.accessToken(this.mvc, this.ana));

		this.mvc
			.perform(post(SESSION).contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(signedOut))
				.content(JsonMapper.shared()
					.writeValueAsString(credentials(this.ana.loginName(), this.ana.password()))))
			.andExpect(status().isOk());
	}

	private String validContent() {
		return JsonMapper.shared().writeValueAsString(credentials(this.ana.loginName(), this.ana.password()));
	}

	private static Map<String, String> credentials(String loginName, String password) {
		return Map.of("loginName", loginName, "password", password);
	}

	private static Map<String, List<String>> headers(MockHttpServletResponse response) {
		return response.getHeaderNames()
			.stream()
			.collect(Collectors.toMap((name) -> name, (name) -> List.copyOf(response.getHeaders(name)), (a, b) -> a,
					TreeMap::new));
	}

}
