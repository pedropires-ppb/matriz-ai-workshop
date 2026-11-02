package com.matriz;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.matriz.Contract.assertProblem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every error is answered with Problem Details that satisfy the contract's Problem schema
 * (ADR-004 decisions 3 and 5; openapi.yaml). A test-only controller, outside {@code /api} and so
 * public, stands in for an operation. Under {@code /api}, the token is checked first (ADR-004,
 * open point "Order of checks").
 */
@AutoConfigureMockMvc
@Import(ProblemDetailsTests.TestOperations.class)
class ProblemDetailsTests extends DatabaseTestSupport {

	private static final String JSON_OPERATION = "/test/json-operation";

	private static final String FAILING_OPERATION = "/test/failing-operation";

	private static final String HIDDEN_CAUSE = "fictitious internal detail";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcClient jdbc;

	@Test
	void theProblemSchemaIsTheContractsAndRejectsAnInvalidProblem() {
		assertThat(Contract.violations("Problem", "{\"title\": \"Not Found\", \"status\": 404, \"detail\": \"Text\"}"))
			.isEmpty();
		assertThat(Contract.violations("Problem", "{\"status\": 200}")).isNotEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "GET", "POST", "PUT", "PATCH", "DELETE" })
	void anUnknownApiPathFirstNeedsAToken(String method) throws Exception {
		TestAccess.assertNoToken(this.mvc.perform(request(HttpMethod.valueOf(method), "/api/unknown")));
	}

	@ParameterizedTest
	@ValueSource(strings = { "GET", "POST", "PUT", "PATCH", "DELETE" })
	void anUnknownApiPathIsNotFoundOnceTheTokenIsAccepted(String method) throws Exception {
		String token = TestAccess.accessToken(this.mvc, TestAccess.addPerson(this.jdbc, "Problem Person"));
		assertProblem(this.mvc.perform(request(HttpMethod.valueOf(method), "/api/unknown")
			.header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(token))), 404, "Not Found",
				"The requested resource does not exist.");
	}

	@Test
	void aMethodThatAnOperationDoesNotSupportIsNotAllowed() throws Exception {
		ResultActions result = this.mvc.perform(get(JSON_OPERATION));
		assertProblem(result, 405, "Method Not Allowed", "Method 'GET' is not supported.");
		result.andExpect(header().string("Allow", "POST"));
	}

	@Test
	void contentInAnotherMediaTypeIsUnsupported() throws Exception {
		assertProblem(this.mvc.perform(post(JSON_OPERATION).contentType(MediaType.TEXT_PLAIN).content("{\"name\": \"x\"}")),
				415, "Unsupported Media Type", "The request content must be in the operation's media type.");
	}

	@Test
	void contentThatIsNotWellFormedJsonIsABadRequest() throws Exception {
		assertProblem(this.mvc.perform(post(JSON_OPERATION).contentType(MediaType.APPLICATION_JSON).content("{\"name\": ")),
				400, "Bad Request", "The request content is not well-formed JSON.");
	}

	@Test
	void wellFormedContentReachesTheOperation() throws Exception {
		this.mvc.perform(post(JSON_OPERATION).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"x\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("x"));
	}

	@Test
	void anUnexpectedErrorRevealsNothingAboutItsCause() throws Exception {
		MvcResult result = assertProblem(this.mvc.perform(get(FAILING_OPERATION)), 500, "Internal Server Error",
				"An unexpected error occurred.");
		assertThat(result.getResponse().getContentAsString()).doesNotContain(HIDDEN_CAUSE)
			.doesNotContain("IllegalStateException");
	}

	@RestController
	static class TestOperations {

		@PostMapping(path = JSON_OPERATION, consumes = MediaType.APPLICATION_JSON_VALUE)
		Map<String, Object> echo(@RequestBody Map<String, Object> content) {
			return content;
		}

		@GetMapping(FAILING_OPERATION)
		String fail() {
			throw new IllegalStateException(HIDDEN_CAUSE);
		}

	}

}
