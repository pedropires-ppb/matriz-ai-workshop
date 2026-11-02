package com.matriz;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.matriz.TestAccess.ME;
import static com.matriz.TestAccess.SESSION;
import static com.matriz.TestAccess.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * No password, password hash, access token, or signing key reaches the logs, from start-up to
 * signing out, even with the most detailed logs of Spring Security, Spring MVC, and Matriz on, and
 * even for a password sent without quotes (ADR-005; Lab Worksheet 03, decision on logging).
 */
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = { "logging.level.org.springframework.security=TRACE",
		"logging.level.org.springframework.web=TRACE", "logging.level.org.springframework.http=TRACE",
		"logging.level.com.matriz=TRACE" })
class LogSecrecyTests extends DatabaseTestSupport {

	/** A fictitious password sent by mistake without quotes, which makes the content invalid JSON. */
	private static final String UNQUOTED_PASSWORD = "fictitiousUnquotedPassword7";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcClient jdbc;

	@Test
	void noPasswordHashTokenOrKeyIsLogged(CapturedOutput output) throws Exception {
		TestAccess.TestPerson rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		String hash = this.jdbc.sql("SELECT password_hash FROM person WHERE person_id = ?")
			.param(rui.id())
			.query(String.class)
			.single();
		String withPassword = "{\"loginName\": \"" + rui.loginName() + "\", \"password\": \"" + rui.password() + "\"";

		String token = TestAccess.accessToken(this.mvc, rui);
		TestAccess.signIn(this.mvc, rui.loginName(), rui.password() + "-wrong").andExpect(status().isUnauthorized());
		signIn(withPassword + ", \"personId\": 1}", MediaType.APPLICATION_JSON_VALUE, 422);
		signIn(withPassword, MediaType.APPLICATION_JSON_VALUE, 400);
		signIn("{\"loginName\": \"" + rui.loginName() + "\", \"password\": " + UNQUOTED_PASSWORD + "}",
				MediaType.APPLICATION_JSON_VALUE, 400);
		signIn(UNQUOTED_PASSWORD, MediaType.APPLICATION_JSON_VALUE, 400);
		signIn(withPassword + "} " + UNQUOTED_PASSWORD, MediaType.APPLICATION_JSON_VALUE, 400);
		signIn(withPassword + "}", MediaType.TEXT_PLAIN_VALUE, 415);
		this.mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(status().isOk());
		this.mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(TestAccess.withAlteredSignature(token))))
			.andExpect(status().isUnauthorized());
		TestAccess.signOut(this.mvc, token);
		this.mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isUnauthorized());

		String claims = token.split("\\.")[1];
		String signature = token.substring(token.lastIndexOf('.') + 1);
		String urlSafeKey = Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(Base64.getDecoder().decode(SIGNING_KEY));
		assertThat(output.getAll()).as("the start-up and the debug logs were captured")
			.contains("Started LogSecrecyTests")
			.contains("/api/session")
			.contains("/api/me")
			.contains("HttpMessageNotReadableException: The sign-in content is not well-formed JSON");
		assertThat(output.getAll()).doesNotContain(rui.password())
			.doesNotContain(UNQUOTED_PASSWORD)
			.doesNotContain(hash)
			.doesNotContain("$2a$")
			.doesNotContain(claims)
			.doesNotContain(signature)
			.doesNotContain(SIGNING_KEY)
			.doesNotContain(urlSafeKey)
			.doesNotContainIgnoringCase("generated security password");
	}

	private void signIn(String content, String mediaType, int expectedStatus) throws Exception {
		this.mvc.perform(post(SESSION).contentType(mediaType).content(content))
			.andExpect(status().is(expectedStatus));
	}

}
