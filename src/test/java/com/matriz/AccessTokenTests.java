package com.matriz;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static com.matriz.Contract.assertProblem;
import static com.matriz.TestAccess.LIFETIME;
import static com.matriz.TestAccess.ME;
import static com.matriz.TestAccess.SESSION;
import static com.matriz.TestAccess.assertInvalidToken;
import static com.matriz.TestAccess.assertNoToken;
import static com.matriz.TestAccess.bearer;
import static com.matriz.TestAccess.matrizClaims;
import static com.matriz.TestAccess.matrizHeader;
import static com.matriz.TestAccess.signedWithTheTestKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Using and ending an access token (openapi.yaml, {@code getCurrentPerson} and {@code signOut};
 * US-013, US-014; ADR-005; ADR-009). The person acting is always the person in the token, and a
 * token that Matriz did not issue unchanged, or that has expired or been signed out, is refused.
 */
@AutoConfigureMockMvc
class AccessTokenTests extends DatabaseTestSupport {

	private static final SecureRandom RANDOM = new SecureRandom();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcClient jdbc;

	private TestAccess.TestPerson ana;

	private String anaToken;

	@BeforeEach
	void signInAna() throws Exception {
		this.ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		this.anaToken = TestAccess.accessToken(this.mvc, this.ana);
	}

	@Test
	void theCurrentPersonIsThePersonInTheToken() throws Exception {
		MvcResult result = me(this.anaToken).andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andReturn();

		String content = result.getResponse().getContentAsString();
		Contract.assertSatisfies("PersonSummary", content);
		JsonNode person = JsonMapper.shared().readTree(content);
		assertThat(person.propertyNames()).containsExactlyInAnyOrder("personId", "displayName");
		assertThat(person.path("personId").stringValue()).isEqualTo(this.ana.id().toString());
		assertThat(person.path("displayName").stringValue()).isEqualTo("Ana Silva");
	}

	@Test
	void anIdentifierSentByTheClientDoesNotChangeTheCurrentPerson() throws Exception {
		TestAccess.TestPerson eva = TestAccess.addPerson(this.jdbc, "Eva Santos");

		this.mvc
			.perform(get(ME).param("personId", eva.id().toString())
				.header(HttpHeaders.AUTHORIZATION, bearer(this.anaToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.personId").value(this.ana.id().toString()));
	}

	@Test
	void twoPeopleEachSeeThemselvesAndSignOutSeparately() throws Exception {
		TestAccess.TestPerson eva = TestAccess.addPerson(this.jdbc, "Eva Santos");
		TestAccess.TestPerson daniel = TestAccess.addPerson(this.jdbc, "Daniel Rocha");
		String evaToken = TestAccess.accessToken(this.mvc, eva);
		String danielToken = TestAccess.accessToken(this.mvc, daniel);

		assertCurrentPerson(this.anaToken, this.ana);
		assertCurrentPerson(evaToken, eva);
		assertCurrentPerson(danielToken, daniel);

		TestAccess.signOut(this.mvc, this.anaToken);

		assertInvalidToken(me(this.anaToken));
		assertCurrentPerson(evaToken, eva);
		assertCurrentPerson(danielToken, daniel);
	}

	@Test
	void signingOutEndsOnlyThePresentedToken() throws Exception {
		String second = TestAccess.accessToken(this.mvc, this.ana);

		MvcResult result = this.mvc.perform(delete(SESSION).header(HttpHeaders.AUTHORIZATION, bearer(this.anaToken)))
			.andExpect(status().isNoContent())
			.andReturn();

		assertThat(result.getResponse().getContentAsByteArray()).isEmpty();
		assertInvalidToken(me(this.anaToken));
		assertCurrentPerson(second, this.ana);
	}

	@Test
	void aSignedOutTokenCannotSignOutAgain() throws Exception {
		TestAccess.signOut(this.mvc, this.anaToken);

		assertInvalidToken(this.mvc.perform(delete(SESSION).header(HttpHeaders.AUTHORIZATION, bearer(this.anaToken))));
	}

	@Test
	void aTokenRecordedAsEndedIsRefusedOnTheNextRequest() throws Exception {
		JsonNode claims = TestAccess.tokenClaims(this.anaToken);
		recordAsEnded(UUID.fromString(claims.path("jti").stringValue()),
				Instant.ofEpochSecond(claims.path("exp").longValue()));

		assertInvalidToken(me(this.anaToken));
	}

	@Test
	void signingOutForgetsTheEndedTokensThatHaveExpired() throws Exception {
		UUID expired = UUID.randomUUID();
		UUID unexpired = UUID.randomUUID();
		recordAsEnded(expired, Instant.now().minusSeconds(60));
		recordAsEnded(unexpired, Instant.now().plusSeconds(3600));

		TestAccess.signOut(this.mvc, this.anaToken);

		assertThat(isRecordedAsEnded(expired)).isFalse();
		assertThat(isRecordedAsEnded(unexpired)).isTrue();
		assertThat(isRecordedAsEnded(UUID.fromString(TestAccess.tokenClaims(this.anaToken).path("jti").stringValue())))
			.isTrue();
	}

	@Test
	void withoutATokenThereIsNoCurrentPersonAndNoSessionToEnd() throws Exception {
		assertNoToken(this.mvc.perform(get(ME)));
		assertNoToken(this.mvc.perform(delete(SESSION)));
	}

	@ParameterizedTest
	@ValueSource(strings = { "Basic ZmljdGl0aW91czp4", "Token abc", "" })
	void anotherAuthorizationSchemeIsNoToken(String authorization) throws Exception {
		assertNoToken(this.mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, authorization)));
	}

	@Test
	void aTokenInTheQueryIsNotRead() throws Exception {
		assertNoToken(this.mvc.perform(get(ME).param("access_token", this.anaToken)));
	}

	@ParameterizedTest
	@ValueSource(strings = { "Bearer", "Bearer ", "Bearer !!!", "Bearer a b" })
	void aMalformedBearerHeaderIsAnInvalidToken(String authorization) throws Exception {
		assertInvalidToken(this.mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, authorization)));
	}

	@Test
	void aTokenMadeAsMatrizMakesThemIsAccepted() throws Exception {
		// The forged tokens below differ from this one only in their forgery.
		assertCurrentPerson(signedWithTheTestKey(matrizHeader(), matrizClaims(this.ana.id())), this.ana);
	}

	static Stream<Named<Forgery>> forgedTokens() {
		return Stream.of(
				Named.of("claims extended after signing",
						(person, token) -> TestAccess.withOtherClaims(token,
								with(claimsOf(token), "exp", Instant.now().plus(LIFETIME.multipliedBy(100)).getEpochSecond()))),
				Named.of("signature altered", (person, token) -> TestAccess.withAlteredSignature(token)),
				Named.of("signed with another key",
						(person, token) -> TestAccess.signed(matrizHeader(), matrizClaims(person), "HmacSHA256",
								randomBytes(32))),
				Named.of("expired an hour ago",
						(person, token) -> signedWithTheTestKey(matrizHeader(),
								expiringAt(person, Instant.now().minusSeconds(3600)))),
				Named.of("expired a second ago, with no clock tolerance",
						(person, token) -> signedWithTheTestKey(matrizHeader(),
								expiringAt(person, Instant.now().minusSeconds(1)))),
				Named.of("signed with HS512",
						(person, token) -> TestAccess.signed(with(matrizHeader(), "alg", "HS512"), matrizClaims(person),
								"HmacSHA512", Base64.getDecoder().decode(DatabaseTestSupport.SIGNING_KEY))),
				Named.of("unsigned, alg none", (person, token) -> TestAccess.unsigned(matrizClaims(person))),
				Named.of("of type at+jwt",
						(person, token) -> signedWithTheTestKey(with(matrizHeader(), "typ", "at+jwt"),
								matrizClaims(person))),
				Named.of("without jti",
						(person, token) -> signedWithTheTestKey(matrizHeader(), without(matrizClaims(person), "jti"))),
				Named.of("jti not a UUID",
						(person, token) -> signedWithTheTestKey(matrizHeader(),
								with(matrizClaims(person), "jti", "not-a-uuid"))),
				Named.of("without sub",
						(person, token) -> signedWithTheTestKey(matrizHeader(), without(matrizClaims(person), "sub"))),
				Named.of("sub not a UUID",
						(person, token) -> signedWithTheTestKey(matrizHeader(),
								with(matrizClaims(person), "sub", "ana"))),
				Named.of("sub in upper case",
						(person, token) -> signedWithTheTestKey(matrizHeader(),
								with(matrizClaims(person), "sub", person.toString().toUpperCase(Locale.ROOT)))),
				Named.of("without iat",
						(person, token) -> signedWithTheTestKey(matrizHeader(), without(matrizClaims(person), "iat"))),
				Named.of("without exp",
						(person, token) -> signedWithTheTestKey(matrizHeader(), without(matrizClaims(person), "exp"))),
				Named.of("not a JSON Web Token", (person, token) -> "not-a-token"),
				Named.of("without its signature part", (person, token) -> token.substring(0, token.lastIndexOf('.'))));
	}

	@ParameterizedTest
	@MethodSource("forgedTokens")
	void aForgedTokenIsRefused(Forgery forgery) throws Exception {
		String forged = forgery.forge(this.ana.id(), this.anaToken);

		MvcResult me = assertInvalidToken(me(forged));
		MvcResult signOut = assertInvalidToken(
				this.mvc.perform(delete(SESSION).header(HttpHeaders.AUTHORIZATION, bearer(forged))));

		assertThat(me.getResponse().getContentAsString()).doesNotContain(forged);
		assertThat(signOut.getResponse().getContentAsString()).doesNotContain(forged);
		assertCurrentPerson(this.anaToken, this.ana);
	}

	@Test
	void aTokenAlteredToNameAnotherPersonIsRefused() throws Exception {
		TestAccess.TestPerson eva = TestAccess.addPerson(this.jdbc, "Eva Santos");

		String altered = TestAccess.withOtherClaims(this.anaToken,
				with(claimsOf(this.anaToken), "sub", eva.id().toString()));

		assertInvalidToken(me(altered));
	}

	@Test
	void aValidTokenForAPersonWhoDoesNotExistIsRefused() throws Exception {
		assertInvalidToken(me(signedWithTheTestKey(matrizHeader(), matrizClaims(UUID.randomUUID()))));
	}

	@ParameterizedTest
	@ValueSource(strings = { "GET", "PUT", "PATCH" })
	void theSessionOnlySupportsSigningInAndOut(String method) throws Exception {
		ResultActions result = this.mvc.perform(
				request(HttpMethod.valueOf(method), SESSION).header(HttpHeaders.AUTHORIZATION, bearer(this.anaToken)));

		assertProblem(result, 405, "Method Not Allowed", "Method '" + method + "' is not supported.");
		assertThat(allowed(result)).containsExactlyInAnyOrder("POST", "DELETE");
	}

	@ParameterizedTest
	@ValueSource(strings = { "POST", "PUT", "PATCH", "DELETE" })
	void theCurrentPersonCanOnlyBeRead(String method) throws Exception {
		ResultActions result = this.mvc.perform(
				request(HttpMethod.valueOf(method), ME).header(HttpHeaders.AUTHORIZATION, bearer(this.anaToken)));

		assertProblem(result, 405, "Method Not Allowed", "Method '" + method + "' is not supported.");
		assertThat(allowed(result)).containsExactly("GET");
	}

	@ParameterizedTest
	@ValueSource(strings = { "GET", "PUT", "PATCH" })
	void anUnsupportedMethodWithoutATokenIsFirstUnauthorized(String method) throws Exception {
		assertNoToken(this.mvc.perform(request(HttpMethod.valueOf(method), SESSION)));
		assertNoToken(this.mvc.perform(request(HttpMethod.valueOf(method), ME)));
	}

	@Test
	void anUnsupportedMethodWithASignedOutTokenIsFirstUnauthorized() throws Exception {
		TestAccess.signOut(this.mvc, this.anaToken);

		assertInvalidToken(this.mvc.perform(get(SESSION).header(HttpHeaders.AUTHORIZATION, bearer(this.anaToken))));
	}

	private ResultActions me(String token) throws Exception {
		return this.mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	private void assertCurrentPerson(String token, TestAccess.TestPerson person) throws Exception {
		me(token).andExpect(status().isOk())
			.andExpect(jsonPath("$.personId").value(person.id().toString()))
			.andExpect(jsonPath("$.displayName").value(person.displayName()));
	}

	private void recordAsEnded(UUID tokenId, Instant expiresAt) {
		this.jdbc.sql("INSERT INTO ended_access_token (token_id, expires_at) VALUES (?, ?)")
			.params(tokenId, OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC))
			.update();
	}

	private boolean isRecordedAsEnded(UUID tokenId) {
		return this.jdbc.sql("SELECT count(*) FROM ended_access_token WHERE token_id = ?")
			.param(tokenId)
			.query(Integer.class)
			.single() == 1;
	}

	private static Set<String> allowed(ResultActions result) {
		String allow = result.andReturn().getResponse().getHeader(HttpHeaders.ALLOW);
		assertThat(allow).isNotNull();
		return Arrays.stream(allow.split(",")).map(String::trim).collect(Collectors.toSet());
	}

	private static Map<String, Object> claimsOf(String token) {
		Map<String, Object> claims = new LinkedHashMap<>();
		TestAccess.tokenClaims(token).properties().forEach((claim) -> claims.put(claim.getKey(), claimValue(claim.getValue())));
		return claims;
	}

	private static Object claimValue(JsonNode value) {
		return value.isNumber() ? value.longValue() : value.stringValue();
	}

	private static Map<String, Object> expiringAt(UUID person, Instant expiresAt) {
		Map<String, Object> claims = matrizClaims(person);
		claims.put("iat", expiresAt.minus(LIFETIME).getEpochSecond());
		claims.put("exp", expiresAt.getEpochSecond());
		return claims;
	}

	private static Map<String, Object> with(Map<String, Object> json, String name, Object value) {
		Map<String, Object> changed = new LinkedHashMap<>(json);
		changed.put(name, value);
		return changed;
	}

	private static Map<String, Object> without(Map<String, Object> json, String name) {
		Map<String, Object> changed = new LinkedHashMap<>(json);
		changed.remove(name);
		return changed;
	}

	private static byte[] randomBytes(int length) {
		byte[] bytes = new byte[length];
		RANDOM.nextBytes(bytes);
		return bytes;
	}

	/** Makes a token from the person's identifier and a token that Matriz issued to them. */
	@FunctionalInterface
	interface Forgery {

		String forge(UUID person, String issuedToken);

	}

}
