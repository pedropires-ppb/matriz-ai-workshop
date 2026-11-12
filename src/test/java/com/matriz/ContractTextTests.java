package com.matriz;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Request schemas must measure approved normalized text, not the length of its padding. */
class ContractTextTests {

	@Test
	void operationsDeclareServerFailuresAndBothPageParametersHaveTheTechnicalBound() {
		int operations = 0;
		int pages = 0;
		for (var path : Contract.document().path("paths").values()) {
			for (var operation : path.values()) {
				if (!operation.has("operationId")) {
					continue;
				}
				operations++;
				assertThat(operation.path("responses").path("500").path("$ref").stringValue())
					.isEqualTo("#/components/responses/ServerError");
				for (var parameter : operation.path("parameters").values()) {
					if ("page".equals(parameter.path("name").stringValue())) {
						pages++;
						assertThat(parameter.path("schema").path("maximum").intValue())
							.isEqualTo(Integer.MAX_VALUE);
					}
				}
			}
		}
		assertThat(operations).isEqualTo(23);
		assertThat(pages).isEqualTo(2);
	}

	static Stream<Arguments> texts() {
		return Stream.of(
				Arguments.of("CreateProjectRequest", "name", 100, true),
				Arguments.of("UpdateProjectRequest", "name", 100, true),
				Arguments.of("CreateProjectRequest", "description", 1000, false),
				Arguments.of("UpdateProjectRequest", "description", 1000, false),
				Arguments.of("CreateTaskRequest", "title", 150, true),
				Arguments.of("UpdateTaskRequest", "title", 150, true),
				Arguments.of("CreateTaskRequest", "description", 2000, false),
				Arguments.of("UpdateTaskRequest", "description", 2000, false),
				Arguments.of("CreateCommentRequest", "text", 2000, true));
	}

	@ParameterizedTest
	@MethodSource("texts")
	void requestsAcceptPaddedUnicodeAtTheLimitAndRejectOverlongOrUnstorableText(
			String schema, String field, int maximum, boolean required) {
		assertValid(schema, field, "\u001c\u00a0" + "😀".repeat(maximum) + "\u202f\u001f", true);
		assertValid(schema, field, "x".repeat(maximum + 1), false);
		assertValid(schema, field, "\u001c\u00a0\u001f", !required);
		assertValid(schema, field, "\ufeff", true);
		assertValid(schema, field, "\ufeff" + "x".repeat(maximum), false);
		for (String text : new String[] { "a\u0000b", "\ud800", "\udfff" }) {
			assertValid(schema, field, text, false);
		}
	}

	private static void assertValid(String schema, String field, String text, boolean valid) {
		Map<String, String> fields = new java.util.LinkedHashMap<>();
		if (schema.equals("CreateProjectRequest")) {
			fields.put("name", "Project");
		}
		if (schema.equals("CreateTaskRequest")) {
			fields.put("title", "Task");
		}
		fields.put(field, text);
		assertThat(Contract.violations(schema, JsonMapper.shared().writeValueAsString(fields)).isEmpty())
			.as("%s.%s accepts the expected text: %s", schema, field, valid).isEqualTo(valid);
	}

}
