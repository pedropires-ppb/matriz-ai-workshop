package com.matriz;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.matriz.Contract.assertProblem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comments on tasks through the API (openapi.yaml, {@code addTaskComment}, {@code listTaskComments};
 * US-021, US-022; EX-023, EX-024, EX-031, EX-032; Assumptions A-023, A-036; ADR-004, ADR-006), with
 * real persistence. Ana is the Project Owner, Rui is a Project Participant, and Daniel is a member of
 * no project.
 */
@AutoConfigureMockMvc
class CommentApiTests extends DatabaseTestSupport {

	private static final String TASK_NOT_FOUND = "The project or task does not exist or is not accessible.";

	private static final String INVALID = "The comment is invalid.";

	private static final String REQUIRED = "The text is required.";

	private static final String TOO_LONG = "The text must have at most 2000 characters.";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcClient jdbc;

	private TestAccess.TestPerson ana;

	private TestAccess.TestPerson rui;

	private String anaToken;

	private String ruiToken;

	private String danielToken;

	private String project;

	private String task;

	@BeforeEach
	void addPeopleAProjectAndATask() throws Exception {
		this.ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		this.rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		TestAccess.TestPerson daniel = TestAccess.addPerson(this.jdbc, "Daniel Reis");
		this.anaToken = TestAccess.accessToken(this.mvc, this.ana);
		this.ruiToken = TestAccess.accessToken(this.mvc, this.rui);
		this.danielToken = TestAccess.accessToken(this.mvc, daniel);
		this.project = createProject(this.anaToken, "Website Redesign");
		this.jdbc.sql("INSERT INTO membership (project_id, person_id, role) VALUES (?, ?, 'PROJECT_PARTICIPANT')")
			.params(id(this.project), this.rui.id())
			.update();
		this.task = createTask(this.project, "Prepare proposal");
	}

	// Adding and listing ---------------------------------------------------------------------

	@Test
	void anEmptyTaskHasNoComments() throws Exception {
		MvcResult result = this.mvc.perform(authorized(get(commentsUrl()), this.anaToken))
			.andExpect(status().isOk())
			.andReturn();

		assertThat(JsonMapper.shared().readTree(result.getResponse().getContentAsString())).isEmpty();
	}

	@Test
	void aMemberAddsACommentWhoseAuthorAndDateTheServerSets() throws Exception {
		Instant before = Instant.now();

		MvcResult result = add(this.ruiToken, "{\"text\":\"Draft sent to the client\"}").andExpect(status().isCreated())
			.andReturn();

		Instant after = Instant.now();
		String body = result.getResponse().getContentAsString();
		Contract.assertSatisfies("Comment", body);
		JsonNode comment = JsonMapper.shared().readTree(body);
		assertThat(comment.path("text").stringValue()).isEqualTo("Draft sent to the client");
		assertThat(comment.path("taskId").stringValue()).isEqualTo(this.task);
		assertThat(comment.path("author").path("personId").stringValue()).isEqualTo(this.rui.id().toString());
		assertThat(comment.path("author").path("displayName").stringValue()).isEqualTo("Rui Martins");
		Instant createdAt = Instant.parse(comment.path("createdAt").stringValue());
		assertThat(createdAt).isBetween(before.minus(Duration.ofSeconds(1)), after.plus(Duration.ofSeconds(1)));
		assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
			.isEqualTo(commentsUrl() + "/" + comment.path("commentId").stringValue());
	}

	@Test
	void theCommentsAreListedOldestFirstWithAuthorAndDateAndStayWhenTheTaskIsOpenedAgain() throws Exception {
		add(this.anaToken, "{\"text\":\"first\"}").andExpect(status().isCreated());
		add(this.ruiToken, "{\"text\":\"second\"}").andExpect(status().isCreated());
		add(this.anaToken, "{\"text\":\"third\"}").andExpect(status().isCreated());

		for (String token : List.of(this.anaToken, this.ruiToken)) {
			MvcResult result = this.mvc.perform(authorized(get(commentsUrl()), token))
				.andExpect(status().isOk())
				.andReturn();
			JsonNode list = JsonMapper.shared().readTree(result.getResponse().getContentAsString());
			assertThat(list).hasSize(3);
			list.forEach((item) -> Contract.assertSatisfies("Comment", item.toString()));
			assertThat(list.valueStream().map((item) -> item.path("text").stringValue()).toList())
				.containsExactly("first", "second", "third");
			assertThat(list.valueStream().map((item) -> item.path("author").path("displayName").stringValue()).toList())
				.containsExactly("Ana Silva", "Rui Martins", "Ana Silva");
		}
		assertThat(count("SELECT count(*) FROM comment WHERE task_id = ?", id(this.task))).isEqualTo(3);
	}

	@Test
	void commentsAddedInTheSameMillisecondKeepTheOrderTheyWereStored() throws Exception {
		for (int number = 1; number <= 5; number++) {
			this.jdbc.sql("INSERT INTO comment (comment_id, task_id, author_person_id, text, created_at)"
					+ " VALUES (?, ?, ?, ?, '2026-11-05T10:00:00Z')")
				.params(UUID.randomUUID(), id(this.task), this.ana.id(), "c" + number)
				.update();
		}

		MvcResult result = this.mvc.perform(authorized(get(commentsUrl()), this.anaToken))
			.andExpect(status().isOk())
			.andReturn();

		assertThat(JsonMapper.shared()
			.readTree(result.getResponse().getContentAsString())
			.valueStream()
			.map((item) -> item.path("text").stringValue())
			.toList()).containsExactly("c1", "c2", "c3", "c4", "c5");
	}

	@Test
	void theAuthorKeepsTheCommentAfterLeavingTheProject() throws Exception {
		add(this.ruiToken, "{\"text\":\"Still mine\"}").andExpect(status().isCreated());
		this.jdbc.sql("DELETE FROM membership WHERE project_id = ? AND person_id = ?")
			.params(id(this.project), this.rui.id())
			.update();

		MvcResult result = this.mvc.perform(authorized(get(commentsUrl()), this.anaToken))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode list = JsonMapper.shared().readTree(result.getResponse().getContentAsString());
		assertThat(list).hasSize(1);
		assertThat(list.get(0).path("author").path("displayName").stringValue()).isEqualTo("Rui Martins");
	}

	// The text ---------------------------------------------------------------------------------

	@Test
	void theTextIsTrimmedAndTheLimitsAre1And2000Characters() throws Exception {
		JsonNode one = created(add(this.anaToken, "{\"text\":\"a\"}"));
		JsonNode longest = created(add(this.anaToken, "{\"text\":\"" + "b".repeat(2000) + "\"}"));
		JsonNode trimmed = created(add(this.anaToken, "{\"text\":\"  \\u00A0 padded \\n\"}"));

		assertThat(one.path("text").stringValue()).isEqualTo("a");
		assertThat(longest.path("text").stringValue()).hasSize(2000);
		assertThat(trimmed.path("text").stringValue()).isEqualTo("padded");
	}

	@Test
	void theLimitCountsCharactersNotBytesOrUtf16Units() throws Exception {
		String emoji = "\uD83D\uDE00";

		created(add(this.anaToken, "{\"text\":\"" + emoji.repeat(2000) + "\"}"));

		assertInvalid(add(this.anaToken, "{\"text\":\"" + emoji.repeat(2001) + "\"}"), TOO_LONG);
	}

	@Test
	void aTextOfMoreThan2000CharactersIsRefused() throws Exception {
		assertInvalid(add(this.anaToken, "{\"text\":\"" + "b".repeat(2001) + "\"}"), TOO_LONG);

		assertThat(count("SELECT count(*) FROM comment WHERE task_id = ?", id(this.task))).isZero();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "   ", "\\t\\n", "\\u00A0", "\\u00A0 \\u2007\\u202F", "\\u3000" })
	void aTextThatIsEmptyOrOnlyWhitespaceIsRefusedIncludingNoBreakSpaces(String text) throws Exception {
		assertInvalid(add(this.anaToken, "{\"text\":\"" + text + "\"}"), REQUIRED);

		assertThat(count("SELECT count(*) FROM comment WHERE task_id = ?", id(this.task))).isZero();
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"text\":null}", "{\"text\":5}", "{\"text\":[\"a\"]}", "[]", "\"text\"", "null" })
	void aMissingOrNotStringTextOrAContentThatIsNotAnObjectIsRefused(String content) throws Exception {
		MvcResult result = assertProblem(add(this.anaToken, content), 422, "Unprocessable Content", INVALID);

		Contract.assertSatisfies("ValidationProblem", result.getResponse().getContentAsString());
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"text\":\"a\",\"createdAt\":\"2020-01-01T00:00:00Z\"}",
			"{\"text\":\"a\",\"author\":{\"personId\":\"00000000-0000-4000-8000-000000000000\"}}",
			"{\"text\":\"a\",\"authorId\":\"x\"}", "{\"text\":\"a\",\"commentId\":\"x\"}" })
	void anAuthorOrADateSentByTheClientIsRefusedAndNothingIsStored(String content) throws Exception {
		MvcResult result = assertProblem(add(this.anaToken, content), 422, "Unprocessable Content", INVALID);

		Contract.assertSatisfies("ValidationProblem", result.getResponse().getContentAsString());
		assertThat(count("SELECT count(*) FROM comment WHERE task_id = ?", id(this.task))).isZero();
	}

	@Test
	void aContentThatIsNotWellFormedIsA400AndAWrongMediaTypeIsA415() throws Exception {
		assertProblem(add(this.anaToken, "{\"text\":"), 400, "Bad Request",
				"The request content is not well-formed JSON.");
		this.mvc.perform(authorized(post(commentsUrl()), this.anaToken).contentType(MediaType.TEXT_PLAIN)
			.content("{\"text\":\"a\"}")).andExpect(status().isUnsupportedMediaType());
		this.mvc.perform(authorized(post(commentsUrl()), this.anaToken).content("{\"text\":\"a\"}"))
			.andExpect(status().isUnsupportedMediaType());
	}

	// Access, states, and statuses ---------------------------------------------------------------

	@Test
	void aMemberOfACompletedProjectCanStillCommentAndRead() throws Exception {
		this.jdbc.sql("UPDATE project SET state = 'COMPLETED' WHERE project_id = ?").param(id(this.project)).update();

		JsonNode byParticipant = created(add(this.ruiToken, "{\"text\":\"Final report approved\"}"));
		JsonNode byOwner = created(add(this.anaToken, "{\"text\":\"Thanks\"}"));

		assertThat(byParticipant.path("text").stringValue()).isEqualTo("Final report approved");
		assertThat(byOwner.path("author").path("displayName").stringValue()).isEqualTo("Ana Silva");
		assertThat(JsonMapper.shared()
			.readTree(this.mvc.perform(authorized(get(commentsUrl()), this.ruiToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString())).hasSize(2);
	}

	@Test
	void aTextThatIsInvalidInACompletedProjectIsStillA422NotA409() throws Exception {
		this.jdbc.sql("UPDATE project SET state = 'COMPLETED' WHERE project_id = ?").param(id(this.project)).update();

		assertInvalid(add(this.anaToken, "{\"text\":\" \"}"), REQUIRED);
	}

	@Test
	void aPersonWhoIsNotAMemberIsToldTheTaskDoesNotExistWhateverTheContent() throws Exception {
		assertProblem(add(this.danielToken, "{\"text\":\"hello\"}"), 404, "Not Found", TASK_NOT_FOUND);
		assertProblem(add(this.danielToken, "{\"text\":\" \"}"), 404, "Not Found", TASK_NOT_FOUND);
		assertProblem(add(this.danielToken, "not json"), 404, "Not Found", TASK_NOT_FOUND);
		assertProblem(this.mvc.perform(authorized(get(commentsUrl()), this.danielToken)), 404, "Not Found",
				TASK_NOT_FOUND);

		assertThat(count("SELECT count(*) FROM comment WHERE task_id = ?", id(this.task))).isZero();
	}

	@Test
	void aTaskOrProjectThatDoesNotExistOrIsNotAUuidIsA404() throws Exception {
		String unknown = UUID.randomUUID().toString();

		for (String url : List.of("/api/projects/" + this.project + "/tasks/" + unknown + "/comments",
				"/api/projects/" + unknown + "/tasks/" + this.task + "/comments",
				"/api/projects/" + this.project + "/tasks/not-a-uuid/comments")) {
			assertProblem(this.mvc.perform(authorized(get(url), this.anaToken)), 404, "Not Found",
					TASK_NOT_FOUND);
			assertProblem(this.mvc.perform(authorized(post(url), this.anaToken).contentType(MediaType.APPLICATION_JSON)
				.content("{\"text\":\"a\"}")), 404, "Not Found", TASK_NOT_FOUND);
		}
	}

	@Test
	void aDeletedTaskOrProjectHasNoCommentsToAddOrRead() throws Exception {
		add(this.anaToken, "{\"text\":\"kept in the database\"}").andExpect(status().isCreated());
		this.jdbc.sql("UPDATE task SET deleted_at = now() WHERE task_id = ?").param(id(this.task)).update();

		assertProblem(add(this.anaToken, "{\"text\":\"a\"}"), 404, "Not Found", TASK_NOT_FOUND);
		assertProblem(this.mvc.perform(authorized(get(commentsUrl()), this.anaToken)), 404, "Not Found",
				TASK_NOT_FOUND);
		assertThat(count("SELECT count(*) FROM comment WHERE task_id = ?", id(this.task))).isEqualTo(1);
	}

	@Test
	void aDeletedProjectHasNoCommentsToAddOrRead() throws Exception {
		this.jdbc.sql("UPDATE project SET deleted_at = now() WHERE project_id = ?").param(id(this.project)).update();

		assertProblem(add(this.anaToken, "{\"text\":\"a\"}"), 404, "Not Found", TASK_NOT_FOUND);
		assertProblem(this.mvc.perform(authorized(get(commentsUrl()), this.anaToken)), 404, "Not Found",
				TASK_NOT_FOUND);
	}

	@Test
	void withoutAValidTokenTheAnswerIs401() throws Exception {
		TestAccess.assertNoToken(this.mvc.perform(get(commentsUrl())));
		TestAccess.assertNoToken(this.mvc
			.perform(post(commentsUrl()).contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"a\"}")));
	}

	@Test
	void aCommentCannotBeEditedOrDeletedThroughTheApi() throws Exception {
		JsonNode comment = created(add(this.anaToken, "{\"text\":\"fixed\"}"));
		String url = commentsUrl() + "/" + comment.path("commentId").stringValue();

		for (HttpMethod method : List.of(HttpMethod.DELETE, HttpMethod.PUT, HttpMethod.PATCH)) {
			this.mvc.perform(authorized(request(method, commentsUrl()), this.anaToken))
				.andExpect(status().isMethodNotAllowed());
			this.mvc.perform(authorized(request(method, url), this.anaToken)).andExpect(status().is4xxClientError());
		}
		assertThat(count("SELECT count(*) FROM comment WHERE task_id = ?", id(this.task))).isEqualTo(1);
	}

	// The database ---------------------------------------------------------------------------------

	@Test
	void theDatabaseRefusesATextOfMoreThan2000CharactersOrAnEmptyOne() {
		assertThatThrownBy(() -> insert("x".repeat(2001))).isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> insert("")).isInstanceOf(DataIntegrityViolationException.class);

		insert("y".repeat(2000));

		assertThat(count("SELECT count(*) FROM comment WHERE task_id = ?", id(this.task))).isEqualTo(1);
	}

	// Helpers ----------------------------------------------------------------------------------------

	private void insert(String text) {
		this.jdbc.sql("INSERT INTO comment (comment_id, task_id, author_person_id, text, created_at)"
				+ " VALUES (?, ?, ?, ?, now())")
			.params(UUID.randomUUID(), id(this.task), this.ana.id(), text)
			.update();
	}

	private String commentsUrl() {
		return "/api/projects/" + this.project + "/tasks/" + this.task + "/comments";
	}

	private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, String token) {
		return request.header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(token));
	}

	private ResultActions add(String token, String content) throws Exception {
		return this.mvc.perform(authorized(post(commentsUrl()), token).contentType(MediaType.APPLICATION_JSON)
			.content(content));
	}

	private JsonNode created(ResultActions actions) throws Exception {
		MvcResult result = actions.andExpect(status().isCreated()).andReturn();
		Contract.assertSatisfies("Comment", result.getResponse().getContentAsString());
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString());
	}

	private void assertInvalid(ResultActions actions, String fieldDetail) throws Exception {
		MvcResult result = assertProblem(actions, 422, "Unprocessable Content", INVALID);
		Contract.assertSatisfies("ValidationProblem", result.getResponse().getContentAsString());
		JsonNode errors = JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("errors");
		assertThat(errors).hasSize(1);
		assertThat(errors.get(0).path("pointer").stringValue()).isEqualTo("/text");
		assertThat(errors.get(0).path("detail").stringValue()).isEqualTo(fieldDetail);
	}

	private String createProject(String token, String name) throws Exception {
		MvcResult result = this.mvc
			.perform(authorized(post("/api/projects"), token).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"" + name + "\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("projectId").stringValue();
	}

	private String createTask(String project, String title) throws Exception {
		MvcResult result = this.mvc
			.perform(authorized(post("/api/projects/" + project + "/tasks"), this.anaToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"" + title + "\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("taskId").stringValue();
	}

	private static UUID id(String text) {
		return UUID.fromString(text);
	}

	private int count(String sql, Object... parameters) {
		return this.jdbc.sql(sql).params(parameters).query(Integer.class).single();
	}

}
