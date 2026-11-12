package com.matriz;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Texts that a person does not see, or that cannot be stored, sent as a project name, a task title,
 * and a comment (US-001, US-011, US-021; Assumptions A-031, A-033, A-036). No decision says whether
 * Matriz accepts a control character, a zero-width character, or half of a surrogate pair, so this
 * test does not choose: whatever Matriz decides, it must answer with a refusal or a creation, never
 * with a server error, and it must keep exactly what it answered, never half of a record.
 */
@AutoConfigureMockMvc
class UnusualTextTests extends DatabaseTestSupport {

	private static final String[] KINDS = { "project name", "task title", "comment" };

	// JSON escapes, so that the text reaches Jackson as a person's client would send it.
	private static final String[] TEXTS = { "a\\u0000b", "\\u0000", "a\\ud800b", "a\\udc00", "\\u200b", "\\u200b\\u200b",
			"a\\u200bb", "a\\u0007b", "a\\u202eb", "\\ufeff", "a\\r\\nb" };

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcClient jdbc;

	private String token;

	private String project;

	private String task;

	@BeforeEach
	void addAPersonAProjectAndATask() throws Exception {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		this.token = TestAccess.accessToken(this.mvc, ana);
		this.project = idOf(post("/api/projects"), "{\"name\":\"Website Redesign\"}", "projectId");
		this.task = idOf(post("/api/projects/" + this.project + "/tasks"), "{\"title\":\"Prepare proposal\"}",
				"taskId");
	}

	@ParameterizedTest(name = "{0}: {1}")
	@MethodSource("unusualTexts")
	void anUnusualTextIsRefusedOrKeptWholeAndNeverAnError(String kind, String escaped) throws Exception {
		int before = records(kind);

		MvcResult result = this.mvc.perform(create(kind, "{\"" + field(kind) + "\":\"" + escaped + "\"}")).andReturn();

		int status = result.getResponse().getStatus();
		String body = result.getResponse().getContentAsString();
		assertThat(status).as("status of %s with %s: %s", kind, escaped, body).isBetween(201, 499).isNotEqualTo(204);
		if (status == 201) {
			String sent = JsonMapper.shared().readTree("{\"v\":\"" + escaped + "\"}").path("v").stringValue();
			String kept = JsonMapper.shared().readTree(body).path(field(kind)).stringValue();
			assertThat(kept).isNotBlank().isEqualTo(sent.strip());
			assertThat(records(kind)).isEqualTo(before + 1);
			assertThat(read(kind)).contains(kept);
		}
		else {
			assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			assertThat(records(kind)).isEqualTo(before);
		}
	}

	static Stream<Arguments> unusualTexts() {
		return Stream.of(KINDS).flatMap((kind) -> Stream.of(TEXTS).map((text) -> Arguments.of(kind, text)));
	}

	// Regression tests for Bug 2 of the verification record: U+0000 gave 500 and an unpaired surrogate
	// was stored as "?" (Assumption A-037).

	@ParameterizedTest(name = "{0}: {1}")
	@MethodSource("textsTheDatabaseCannotKeep")
	void aTextTheDatabaseCannotKeepIsRefusedOnItsFieldAndNothingIsStored(String kind, String escaped) throws Exception {
		int before = records(kind);

		MvcResult result = this.mvc.perform(create(kind, "{\"" + field(kind) + "\":\"" + escaped + "\"}")).andReturn();

		assertRefusedOnItsField(result, "/" + field(kind),
				"The " + word(kind) + " contains a character that cannot be stored.");
		assertThat(records(kind)).isEqualTo(before);
	}

	static Stream<Arguments> textsTheDatabaseCannotKeep() {
		return Stream.of(KINDS)
			.flatMap((kind) -> Stream.of("a\\u0000b", "\\u0000", "a\\ud800b", "a\\udc00").map((text) -> Arguments.of(kind, text)));
	}

	@ParameterizedTest
	@ValueSource(strings = { "a\\u0000b", "a\\ud83db" })
	void aDescriptionTheDatabaseCannotKeepIsRefusedWithNothingStored(String escaped) throws Exception {
		String project = "{\"name\":\"Other\",\"description\":\"" + escaped + "\"}";
		String task = "{\"title\":\"Other\",\"description\":\"" + escaped + "\"}";
		int projects = records("project name");
		int tasks = records("task title");

		assertRefusedOnItsField(this.mvc.perform(create("project name", project)).andReturn(), "/description",
				"The description contains a character that cannot be stored.");
		assertRefusedOnItsField(this.mvc.perform(create("task title", task)).andReturn(), "/description",
				"The description contains a character that cannot be stored.");

		assertThat(records("project name")).isEqualTo(projects);
		assertThat(records("task title")).isEqualTo(tasks);
	}

	@ParameterizedTest
	@ValueSource(strings = { "a\\u0000b", "a\\ud800b" })
	void anEditWithATextTheDatabaseCannotKeepRefusesItAndKeepsTheOldValues(String escaped) throws Exception {
		MvcResult project = this.mvc
			.perform(authorized(patch("/api/projects/" + this.project)).contentType("application/merge-patch+json")
				.content("{\"name\":\"" + escaped + "\"}"))
			.andReturn();
		MvcResult task = this.mvc
			.perform(authorized(patch("/api/projects/" + this.project + "/tasks/" + this.task))
				.contentType("application/merge-patch+json")
				.content("{\"title\":\"" + escaped + "\"}"))
			.andReturn();

		assertRefusedOnItsField(project, "/name", "The name contains a character that cannot be stored.");
		assertRefusedOnItsField(task, "/title", "The title contains a character that cannot be stored.");
		assertThat(this.jdbc.sql("SELECT name FROM project WHERE project_id = ?")
			.param(UUID.fromString(this.project))
			.query(String.class)
			.single()).isEqualTo("Website Redesign");
		assertThat(this.jdbc.sql("SELECT title FROM task WHERE task_id = ?")
			.param(UUID.fromString(this.task))
			.query(String.class)
			.single()).isEqualTo("Prepare proposal");
	}

	@ParameterizedTest
	@MethodSource("searchTextsTheDatabaseCannotKeep")
	void aSearchTextTheDatabaseCannotKeepIsABadRequest(String path, String text) throws Exception {
		MvcResult result = this.mvc
			.perform(authorized(get(path.replace("{project}", this.project))).param("name", text))
			.andReturn();

		assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(400);
		assertThat(JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("detail").stringValue())
			.isEqualTo("The query parameter name must not contain a character that cannot be stored.");
	}

	static Stream<Arguments> searchTextsTheDatabaseCannotKeep() {
		return Stream.of("/api/projects", "/api/projects/{project}/eligible-participants")
			.flatMap((path) -> Stream.of("a\u0000b", "a\ud800b").map((text) -> Arguments.of(path, text)));
	}

	@ParameterizedTest
	@ValueSource(strings = { "a\\u0000b", "a\\ud800b" })
	void aSignInWithATextTheDatabaseCannotKeepIsAnIncorrectLoginLikeAnUnknownLoginName(String escaped) throws Exception {
		MvcResult result = this.mvc
			.perform(post(TestAccess.SESSION).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginName\":\"" + escaped + "\",\"password\":\"fictitious\"}"))
			.andReturn();
		MvcResult unknown = this.mvc
			.perform(post(TestAccess.SESSION).contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginName\":\"nobody\",\"password\":\"fictitious\"}"))
			.andReturn();

		assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(401);
		assertThat(result.getResponse().getContentAsString()).isEqualTo(unknown.getResponse().getContentAsString());
	}

	private static void assertRefusedOnItsField(MvcResult result, String pointer, String detail) throws Exception {
		String body = result.getResponse().getContentAsString();
		assertThat(result.getResponse().getStatus()).as(body).isEqualTo(422);
		Contract.assertSatisfies("ValidationProblem", body);
		JsonNode errors = JsonMapper.shared().readTree(body).path("errors");
		assertThat(errors).hasSize(1);
		assertThat(errors.get(0).path("pointer").stringValue()).isEqualTo(pointer);
		assertThat(errors.get(0).path("detail").stringValue()).isEqualTo(detail);
	}

	private static String word(String kind) {
		return switch (kind) {
			case "project name" -> "name";
			case "task title" -> "title";
			default -> "text";
		};
	}

	private MockHttpServletRequestBuilder create(String kind, String content) {
		String url = switch (kind) {
			case "project name" -> "/api/projects";
			case "task title" -> "/api/projects/" + this.project + "/tasks";
			default -> "/api/projects/" + this.project + "/tasks/" + this.task + "/comments";
		};
		return authorized(post(url)).contentType(MediaType.APPLICATION_JSON).content(content);
	}

	private static String field(String kind) {
		return switch (kind) {
			case "project name" -> "name";
			case "task title" -> "title";
			default -> "text";
		};
	}

	/** What a person reads back: the project names, the task titles, or the comment texts. */
	private String read(String kind) throws Exception {
		String url = switch (kind) {
			case "project name" -> "/api/projects";
			case "task title" -> "/api/projects/" + this.project + "/tasks";
			default -> "/api/projects/" + this.project + "/tasks/" + this.task + "/comments";
		};
		String body = this.mvc.perform(authorized(get(url).param("size", "100")))
			.andReturn()
			.getResponse()
			.getContentAsString();
		JsonNode root = JsonMapper.shared().readTree(body);
		JsonNode items = root.has("items") ? root.path("items") : root;
		StringBuilder texts = new StringBuilder();
		items.forEach((item) -> texts.append(item.path(field(kind)).stringValue()).append('\n'));
		return texts.toString();
	}

	private int records(String kind) {
		return switch (kind) {
			case "project name" -> count("SELECT count(*) FROM project");
			case "task title" -> count("SELECT count(*) FROM task");
			default -> count("SELECT count(*) FROM comment");
		};
	}

	private int count(String sql) {
		return this.jdbc.sql(sql).query(Integer.class).single();
	}

	private String idOf(MockHttpServletRequestBuilder request, String content, String property) throws Exception {
		MvcResult result = this.mvc.perform(authorized(request).contentType(MediaType.APPLICATION_JSON).content(content))
			.andReturn();
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path(property).stringValue();
	}

	private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(this.token));
	}

}
