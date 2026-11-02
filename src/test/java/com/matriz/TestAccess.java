package com.matriz;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * People and access tokens for the tests (ADR-005; ADR-009). People are added with fictitious
 * passwords, hashed as the demonstration data hashes them. Tokens are obtained by signing in, or
 * made by a test with the test signing key, to present tokens that Matriz did not issue.
 */
final class TestAccess {

	static final String SESSION = "/api/session";

	static final String ME = "/api/me";

	static final String NO_TOKEN = "Identity has not been established.";

	static final String INVALID_TOKEN = "The access token is invalid, expired, or no longer accepted.";

	static final String INCORRECT_CREDENTIALS = "The login name or password is incorrect.";

	static final String BEARER_CHALLENGE = "Bearer";

	static final String INVALID_TOKEN_CHALLENGE = "Bearer error=\"invalid_token\"";

	/** The agreed token lifetime (ADR-005, open point "Token lifetime"). */
	static final Duration LIFETIME = Duration.ofHours(8);

	private static final PasswordEncoder PASSWORD_ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	private static final AtomicInteger PEOPLE = new AtomicInteger();

	private TestAccess() {
	}

	/**
	 * Adds a person with this display name, a login name no other test uses, and a fictitious
	 * password.
	 */
	static TestPerson addPerson(JdbcClient jdbc, String displayName) {
		String loginName = "test-" + PEOPLE.incrementAndGet() + "-"
				+ displayName.toLowerCase(Locale.ROOT).replace(' ', '.');
		TestPerson person = new TestPerson(UUID.randomUUID(), loginName, displayName,
				"fictitious-" + UUID.randomUUID());
		jdbc.sql("INSERT INTO person (person_id, login_name, display_name, password_hash) VALUES (?, ?, ?, ?)")
			.params(person.id(), person.loginName(), person.displayName(), PASSWORD_ENCODER.encode(person.password()))
			.update();
		return person;
	}

	/** {@code POST /api/session} with this login name and password. */
	static ResultActions signIn(MockMvc mvc, String loginName, String password) throws Exception {
		return mvc.perform(post(SESSION).contentType(MediaType.APPLICATION_JSON)
			.content(JsonMapper.shared().writeValueAsString(Map.of("loginName", loginName, "password", password))));
	}

	/** Signs the person in and returns the access token. */
	static String accessToken(MockMvc mvc, TestPerson person) throws Exception {
		MvcResult result = signIn(mvc, person.loginName(), person.password()).andExpect(status().isOk()).andReturn();
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("accessToken").stringValue();
	}

	/** Signs out with the token, which is then no longer accepted, and returns it. */
	static String signOut(MockMvc mvc, String token) throws Exception {
		mvc.perform(delete(SESSION).header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isNoContent());
		return token;
	}

	/** The value of an {@code Authorization} header that presents the token. */
	static String bearer(String token) {
		return "Bearer " + token;
	}

	/** Asserts the 401 answer to a request without a token (openapi.yaml, Unauthorized). */
	static MvcResult assertNoToken(ResultActions actions) throws Exception {
		actions.andExpect(header().stringValues(HttpHeaders.WWW_AUTHENTICATE, BEARER_CHALLENGE));
		return Contract.assertProblem(actions, 401, "Unauthorized", NO_TOKEN);
	}

	/** Asserts the 401 answer to a request with a refused token (openapi.yaml, Unauthorized). */
	static MvcResult assertInvalidToken(ResultActions actions) throws Exception {
		actions.andExpect(header().stringValues(HttpHeaders.WWW_AUTHENTICATE, INVALID_TOKEN_CHALLENGE));
		return Contract.assertProblem(actions, 401, "Unauthorized", INVALID_TOKEN);
	}

	/** The header of a token, read without verifying it. */
	static JsonNode tokenHeader(String token) {
		return part(token, 0);
	}

	/** The claims of a token, read without verifying it. */
	static JsonNode tokenClaims(String token) {
		return part(token, 1);
	}

	/** The header of the tokens that Matriz issues. */
	static Map<String, Object> matrizHeader() {
		Map<String, Object> header = new LinkedHashMap<>();
		header.put("alg", "HS256");
		header.put("typ", "JWT");
		return header;
	}

	/** The claims of a token that Matriz would issue to the person now. */
	static Map<String, Object> matrizClaims(UUID person) {
		Instant now = Instant.now();
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("sub", person.toString());
		claims.put("jti", UUID.randomUUID().toString());
		claims.put("iat", now.getEpochSecond());
		claims.put("exp", now.plus(LIFETIME).getEpochSecond());
		return claims;
	}

	/** A token signed with the test signing key and HMAC SHA-256, as Matriz signs its tokens. */
	static String signedWithTheTestKey(Map<String, ?> header, Map<String, ?> claims) {
		return signed(header, claims, "HmacSHA256", Base64.getDecoder().decode(DatabaseTestSupport.SIGNING_KEY));
	}

	/** A token signed with HMAC: macAlgorithm is a Java name, such as HmacSHA512. */
	static String signed(Map<String, ?> header, Map<String, ?> claims, String macAlgorithm, byte[] key) {
		String signingInput = encoded(header) + "." + encoded(claims);
		try {
			Mac mac = Mac.getInstance(macAlgorithm);
			mac.init(new SecretKeySpec(key, macAlgorithm));
			byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
			return signingInput + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/** An unsecured token, {@code alg: none}, which has no signature. */
	static String unsigned(Map<String, ?> claims) {
		return encoded(Map.of("alg", "none")) + "." + encoded(claims) + ".";
	}

	/** The token with other claims, but its header and signature unchanged. */
	static String withOtherClaims(String token, Map<String, ?> claims) {
		String[] parts = token.split("\\.");
		return parts[0] + "." + encoded(claims) + "." + parts[2];
	}

	/**
	 * The token with the first character of its signature changed. The last character could
	 * differ only in padding bits, which do not change the signature.
	 */
	static String withAlteredSignature(String token) {
		int signature = token.lastIndexOf('.') + 1;
		char altered = (token.charAt(signature) == 'A') ? 'B' : 'A';
		return token.substring(0, signature) + altered + token.substring(signature + 1);
	}

	private static String encoded(Map<String, ?> json) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(JsonMapper.shared().writeValueAsBytes(json));
	}

	private static JsonNode part(String token, int index) {
		return JsonMapper.shared().readTree(Base64.getUrlDecoder().decode(token.split("\\.")[index]));
	}

	/** A person added by a test, with their fictitious password. */
	record TestPerson(UUID id, String loginName, String displayName, String password) {

	}

}
