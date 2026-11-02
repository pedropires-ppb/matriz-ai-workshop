package com.matriz;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guard 2 of ADR-009 decision 3: the contract lets no client name the person acting, such as a
 * project's owner or a comment's author. Each request content is a reviewed schema that allows
 * exactly its reviewed properties, and a person's identifier is taken only where the client
 * chooses a person: the participant to add or remove, and the person to assign or unassign.
 * Changing this list needs a review against ADR-009.
 */
class RequestSchemaTests {

	private static final String SCHEMAS = "#/components/schemas/";

	/** The media types of request content: JSON, and JSON Merge Patch for partial updates. */
	private static final Set<String> MEDIA_TYPES = Set.of("application/json", "application/merge-patch+json");

	/** The request schemas and the properties each one allows, as reviewed. */
	private static final Map<String, Set<String>> REVIEWED_SCHEMAS = Map.of("SignInRequest",
			Set.of("loginName", "password"), "CreateProjectRequest", Set.of("name"), "RenameProjectRequest",
			Set.of("name"), "CompleteProjectRequest", Set.of("state"), "AddParticipantRequest", Set.of("personId"),
			"CreateTaskRequest", Set.of("title", "priority"), "UpdateTaskRequest", Set.of("title", "priority"),
			"ChangeTaskStateRequest", Set.of("state"), "AssignTaskRequest", Set.of("personId"),
			"CreateCommentRequest", Set.of("text"));

	/** The only schemas that take a person's identifier: the person chosen, never the person acting. */
	private static final Set<String> CHOOSE_A_PERSON = Set.of("AddParticipantRequest", "AssignTaskRequest");

	/** The parameters of all operations, as reviewed: {@code in} and {@code name}. */
	private static final Set<String> REVIEWED_PARAMETERS = Set.of("path projectId", "path taskId", "path personId",
			"query name", "query state", "query priority");

	/** The only paths with a person's identifier: the participant to remove, and the person to unassign. */
	private static final Set<String> PATHS_WITH_A_PERSON = Set.of("/projects/{projectId}/members/{personId}",
			"/projects/{projectId}/tasks/{taskId}/assignees/{personId}");

	private static final List<String> COMBINING_KEYWORDS = List.of("allOf", "anyOf", "oneOf", "not", "$ref",
			"patternProperties", "dependentSchemas", "unevaluatedProperties", "if");

	@Test
	void everyRequestContentIsAReviewedSchema() {
		Set<String> used = new TreeSet<>();
		for (JsonNode path : Contract.document().path("paths").values()) {
			for (JsonNode operation : path.values()) {
				if (operation.has("requestBody")) {
					String id = operation.path("operationId").stringValue();
					JsonNode content = operation.path("requestBody").path("content");
					assertThat(content.propertyNames()).as(id).singleElement().isIn(MEDIA_TYPES);
					JsonNode schema = content.values().iterator().next().path("schema");
					assertThat(schema.propertyNames()).as(id).containsExactly("$ref");
					String reference = schema.path("$ref").stringValue();
					assertThat(reference).as(id).startsWith(SCHEMAS);
					used.add(reference.substring(SCHEMAS.length()));
				}
			}
		}
		assertThat(used).isEqualTo(new TreeSet<>(REVIEWED_SCHEMAS.keySet()));
	}

	static Stream<String> reviewedSchemas() {
		return REVIEWED_SCHEMAS.keySet().stream().sorted();
	}

	@ParameterizedTest
	@MethodSource("reviewedSchemas")
	void aRequestSchemaAllowsExactlyItsReviewedProperties(String name) {
		JsonNode schema = Contract.document().path("components").path("schemas").path(name);

		assertThat(schema.path("type").stringValue()).as(name).isEqualTo("object");
		assertThat(schema.path("additionalProperties").isBoolean()).as(name).isTrue();
		assertThat(schema.path("additionalProperties").booleanValue()).as(name).isFalse();
		COMBINING_KEYWORDS.forEach((keyword) -> assertThat(schema.has(keyword)).as(name + " " + keyword).isFalse());
		assertThat(schema.path("properties").propertyNames()).as(name)
			.containsExactlyInAnyOrderElementsOf(REVIEWED_SCHEMAS.get(name));
	}

	@Test
	void onlyAddingAParticipantAndAssigningATaskTakeAPersonInTheirContent() {
		REVIEWED_SCHEMAS.forEach((name, properties) -> {
			boolean takesAPerson = properties.stream().anyMatch(RequestSchemaTests::namesAPerson);
			assertThat(takesAPerson).as(name).isEqualTo(CHOOSE_A_PERSON.contains(name));
		});
	}

	@Test
	void everyParameterIsReviewedAndOnlyChosenPeopleAreInThePath() {
		JsonNode document = Contract.document();
		Set<String> parameters = new HashSet<>();
		for (Map.Entry<String, JsonNode> path : document.path("paths").properties()) {
			Set<String> ofPath = new HashSet<>();
			List<JsonNode> declared = new ArrayList<>(path.getValue().path("parameters").values());
			for (JsonNode operation : path.getValue().values()) {
				declared.addAll(operation.path("parameters").values());
			}
			for (JsonNode parameter : declared) {
				JsonNode resolved = resolve(document, parameter);
				String parameterName = resolved.path("in").stringValue() + " " + resolved.path("name").stringValue();
				ofPath.add(parameterName);
				parameters.add(parameterName);
			}
			assertThat(ofPath.contains("path personId")).as(path.getKey())
				.isEqualTo(PATHS_WITH_A_PERSON.contains(path.getKey()));
		}
		assertThat(parameters).isEqualTo(REVIEWED_PARAMETERS);
	}

	private static boolean namesAPerson(String property) {
		String lowerCase = property.toLowerCase(Locale.ROOT);
		return lowerCase.contains("person") || lowerCase.contains("owner") || lowerCase.contains("author")
				|| lowerCase.contains("user") || lowerCase.endsWith("by");
	}

	private static JsonNode resolve(JsonNode document, JsonNode parameter) {
		if (!parameter.has("$ref")) {
			return parameter;
		}
		String reference = parameter.path("$ref").stringValue();
		assertThat(reference).startsWith("#/components/parameters/");
		return document.path("components").path("parameters").path(reference.substring(reference.lastIndexOf('/') + 1));
	}

}
