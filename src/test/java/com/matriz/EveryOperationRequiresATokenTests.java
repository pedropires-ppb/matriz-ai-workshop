package com.matriz;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Guard 1 of ADR-009 decision 3: every operation of the contract except signing in needs an
 * accepted access token, also when it is requested directly. The operations are read from
 * openapi.yaml, so an operation added to the contract is guarded at once, before it is
 * implemented: without a token, or with a signed-out one, it gets {@code 401} first (ADR-004,
 * order of checks).
 */
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EveryOperationRequiresATokenTests extends DatabaseTestSupport {

	private static final Set<String> METHODS = Set.of("get", "put", "post", "delete", "options", "head", "patch",
			"trace");

	/** A fictitious identifier for every path parameter. */
	private static final String FICTITIOUS_ID = "99999999-9999-4999-8999-999999999999";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcClient jdbc;

	private String signedOutToken;

	@BeforeAll
	void signOutAToken() throws Exception {
		this.signedOutToken = TestAccess.signOut(this.mvc,
				TestAccess.accessToken(this.mvc, TestAccess.addPerson(this.jdbc, "Guard Person")));
	}

	@Test
	void signingInIsTheOnlyOperationWithoutAToken() {
		JsonNode document = Contract.document();
		assertThat(document.path("security").values()).hasSize(1);
		assertThat(document.path("security").path(0).propertyNames()).containsExactly("bearerAuth");
		assertThat(document.path("components").path("securitySchemes").path("bearerAuth").path("scheme").stringValue())
			.isEqualTo("bearer");
		assertThat(operations().filter(Operation::isPublic).map(Operation::id)).containsExactly("signIn");
		assertThat(operations().filter((operation) -> !operation.isPublic())).hasSize(22);
	}

	@ParameterizedTest
	@MethodSource("guardedOperations")
	void withoutATokenAnOperationIsRefused(Operation operation) throws Exception {
		TestAccess.assertNoToken(this.mvc.perform(operation.toRequest()));
	}

	@ParameterizedTest
	@MethodSource("guardedOperations")
	void withASignedOutTokenAnOperationIsRefused(Operation operation) throws Exception {
		TestAccess.assertInvalidToken(this.mvc
			.perform(operation.toRequest().header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(this.signedOutToken))));
	}

	static Stream<Named<Operation>> guardedOperations() {
		return operations().filter((operation) -> !operation.isPublic())
			.map((operation) -> Named.of(operation.id() + " " + operation.method() + " " + operation.path(),
					operation));
	}

	/** Every operation of the contract, with its path under the server's base path. */
	static Stream<Operation> operations() {
		JsonNode document = Contract.document();
		String basePath = document.path("servers").path(0).path("url").stringValue();
		List<Operation> operations = new ArrayList<>();
		for (Map.Entry<String, JsonNode> path : document.path("paths").properties()) {
			for (Map.Entry<String, JsonNode> member : path.getValue().properties()) {
				if (METHODS.contains(member.getKey())) {
					JsonNode operation = member.getValue();
					operations.add(new Operation(operation.path("operationId").stringValue(),
							member.getKey().toUpperCase(Locale.ROOT), basePath + path.getKey(),
							operation.has("security") && operation.path("security").isEmpty(),
							mediaType(operation)));
				}
			}
		}
		return operations.stream();
	}

	/** The media type of the operation's request content, or {@code null} if it has none. */
	private static String mediaType(JsonNode operation) {
		JsonNode content = operation.path("requestBody").path("content");
		return content.isEmpty() ? null : content.propertyNames().iterator().next();
	}

	/** An operation of the contract. */
	record Operation(String id, String method, String path, boolean isPublic, String mediaType) {

		/**
		 * A request for the operation, with a fictitious identifier for each path parameter and an
		 * empty object as content where the operation takes content.
		 */
		MockHttpServletRequestBuilder toRequest() {
			MockHttpServletRequestBuilder request = request(HttpMethod.valueOf(this.method),
					this.path.replaceAll("\\{[^}]+}", FICTITIOUS_ID));
			return (this.mediaType != null) ? request.contentType(MediaType.parseMediaType(this.mediaType)).content("{}")
					: request;
		}

	}

}
