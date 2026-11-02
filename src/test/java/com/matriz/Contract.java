package com.matriz;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.networknt.schema.InputFormat;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialect;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.keyword.AnnotationKeyword;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The API contract, openapi.yaml, as the tests read it (ADR-004 decision 1): its document, and
 * its schemas to check responses against.
 */
final class Contract {

	static final Path FILE = Path.of("openapi.yaml");

	private static final String IRI = "https://matriz.invalid/openapi.yaml";

	private static final SchemaRegistry SCHEMAS = schemas();

	private Contract() {
	}

	/** The whole contract, as a tree. */
	static JsonNode document() {
		return YAMLMapper.shared().readTree(FILE);
	}

	/** What in the JSON content does not satisfy the schema of that name in the contract. */
	static List<com.networknt.schema.Error> violations(String schemaName, String json) {
		return SCHEMAS.getSchema(SchemaLocation.of(IRI + "#/components/schemas/" + schemaName))
			.validate(json, InputFormat.JSON);
	}

	/** Asserts that the JSON content satisfies the schema of that name in the contract. */
	static void assertSatisfies(String schemaName, String json) {
		assertThat(violations(schemaName, json)).as("%s satisfies %s", json, schemaName).isEmpty();
	}

	/**
	 * Asserts that the response is Problem Details with this status, title, and detail, without a
	 * type, and that it satisfies the contract's Problem schema (openapi.yaml, Errors).
	 */
	static MvcResult assertProblem(ResultActions actions, int status, String title, String detail) throws Exception {
		MvcResult result = actions.andExpect(status().is(status))
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(status))
			.andExpect(jsonPath("$.title").value(title))
			.andExpect(jsonPath("$.detail").value(detail))
			.andExpect(jsonPath("$.type").doesNotExist())
			.andReturn();
		assertSatisfies("Problem", result.getResponse().getContentAsString());
		return result;
	}

	private static SchemaRegistry schemas() {
		try {
			String contract = Files.readString(FILE);
			// The document's own fields (openapi, info, paths, ...) are not schema keywords: read them as
			// annotations, as the default factory does, but without logging a warning for each.
			Dialect dialect = Dialect.builder(Dialects.getDraft202012())
				.unknownKeywordFactory((keyword, context) -> new AnnotationKeyword(keyword))
				.build();
			return SchemaRegistry.withDefaultDialect(dialect, (builder) -> builder.schemas(Map.of(IRI, contract)));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
