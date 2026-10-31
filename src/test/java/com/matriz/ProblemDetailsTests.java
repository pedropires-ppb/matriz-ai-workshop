package com.matriz;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialect;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.keyword.AnnotationKeyword;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every error is answered with Problem Details that satisfy the contract's Problem schema
 * (ADR-004 decisions 3 and 5; openapi.yaml). I1 has no API operation yet, so a test-only
 * controller stands in for one.
 */
@AutoConfigureMockMvc
@Import(ProblemDetailsTests.TestOperations.class)
class ProblemDetailsTests extends DatabaseTestSupport {

	private static final String JSON_OPERATION = "/test/json-operation";

	private static final String FAILING_OPERATION = "/test/failing-operation";

	private static final String HIDDEN_CAUSE = "fictitious internal detail";

	private static final Schema PROBLEM = problemSchema();

	@Autowired
	private MockMvc mvc;

	@Test
	void theProblemSchemaIsTheContractsAndRejectsAnInvalidProblem() {
		assertThat(PROBLEM.validate("{\"title\": \"Not Found\", \"status\": 404, \"detail\": \"Text\"}", InputFormat.JSON))
			.isEmpty();
		assertThat(PROBLEM.validate("{\"status\": 200}", InputFormat.JSON)).isNotEmpty();
	}

	@Test
	void anUnknownPathIsNotFound() throws Exception {
		assertProblem(this.mvc.perform(get("/api/unknown")), 404, "Not Found", "The requested resource does not exist.");
	}

	@ParameterizedTest
	@ValueSource(strings = { "POST", "PUT", "PATCH", "DELETE" })
	void otherMethodsOnAnUnknownPathAreNotFoundToo(String method) throws Exception {
		assertProblem(this.mvc.perform(request(HttpMethod.valueOf(method), "/api/unknown")), 404, "Not Found",
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

	private static MvcResult assertProblem(ResultActions actions, int status, String title, String detail)
			throws Exception {
		MvcResult result = actions.andExpect(status().is(status))
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(status))
			.andExpect(jsonPath("$.title").value(title))
			.andExpect(jsonPath("$.detail").value(detail))
			.andExpect(jsonPath("$.type").doesNotExist())
			.andReturn();
		assertThat(PROBLEM.validate(result.getResponse().getContentAsString(), InputFormat.JSON)).isEmpty();
		return result;
	}

	private static Schema problemSchema() {
		String iri = "https://matriz.invalid/openapi.yaml";
		try {
			String contract = Files.readString(Path.of("openapi.yaml"));
			// The document's own fields (openapi, info, paths, ...) are not schema keywords: read them as
			// annotations, as the default factory does, but without logging a warning for each.
			Dialect dialect = Dialect.builder(Dialects.getDraft202012())
				.unknownKeywordFactory((keyword, context) -> new AnnotationKeyword(keyword))
				.build();
			SchemaRegistry registry = SchemaRegistry.withDefaultDialect(dialect,
					(builder) -> builder.schemas(Map.of(iri, contract)));
			return registry.getSchema(SchemaLocation.of(iri + "#/components/schemas/Problem"));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
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
