package com.matriz;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.matriz.Contract.assertProblem;
import static com.matriz.SimultaneousRequests.simultaneously;
import static com.matriz.SimultaneousRequests.statusOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Completing a project through the API (openapi.yaml, {@code completeProject}; FR-007; US-010; EX-006,
 * EX-018; Assumptions A-001, A-007, A-011, A-023, A-029; ADR-006), with real persistence. The I8
 * decisions 2, 3, 7, and 8 in the implementation plan.
 */
@AutoConfigureMockMvc
class CompletionApiTests extends DatabaseTestSupport {

	private static final String PROJECTS = "/api/projects";

	private static final MediaType MERGE_PATCH = MediaType.parseMediaType("application/merge-patch+json");

	private static final String NOT_FOUND = "The project does not exist or is not accessible.";

	private static final String NOT_OWNER = "Only the Project Owner of this project can do this.";

	private static final String COMPLETED = "The project is Completed; it cannot be changed.";

	private static final String UNFINISHED = "The project has tasks that are To do or In progress; it remains Active.";

	private static final String ONLY_COMPLETED = "A project can only be changed to the state COMPLETED.";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcClient jdbc;

	private TestAccess.TestPerson ana;

	private TestAccess.TestPerson rui;

	private TestAccess.TestPerson eva;

	private String anaToken;

	private String ruiToken;

	private String evaToken;

	private String project;

	@BeforeEach
	void addPeopleAndAProject() throws Exception {
		this.ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		this.rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		this.eva = TestAccess.addPerson(this.jdbc, "Eva Costa");
		this.anaToken = TestAccess.accessToken(this.mvc, this.ana);
		this.ruiToken = TestAccess.accessToken(this.mvc, this.rui);
		this.evaToken = TestAccess.accessToken(this.mvc, this.eva);
		this.project = createProject("Alpha");
		addMember(this.rui);
	}

	// Completing -------------------------------------------------------------------------------

	@Test
	void aProjectWhoseTasksAreAllDoneIsCompleted() throws Exception {
		addTask("DONE");
		addTask("DONE");

		MvcResult result = complete(this.anaToken).andExpect(status().isOk()).andReturn();

		Contract.assertSatisfies("Project", result.getResponse().getContentAsString());
		JsonNode body = body(result);
		assertThat(body.path("state").stringValue()).isEqualTo("COMPLETED");
		assertThat(body.path("myRole").stringValue()).isEqualTo("PROJECT_OWNER");
		assertThat(body.path("progress").path("taskCounts").path("done").asInt()).isEqualTo(2);
		assertThat(body.path("progress").path("percentDone").asInt()).isEqualTo(100);
		assertThat(storedState()).isEqualTo("COMPLETED");
		assertThat(view(this.ruiToken).path("state").stringValue()).isEqualTo("COMPLETED");
	}

	@Test
	void aProjectWithNoTasksIsCompleted() throws Exception {
		JsonNode body = body(complete(this.anaToken).andExpect(status().isOk()).andReturn());

		assertThat(body.path("state").stringValue()).isEqualTo("COMPLETED");
		assertThat(body.path("progress").path("percentDone").isNull()).isTrue();
		assertThat(storedState()).isEqualTo("COMPLETED");
	}

	@ParameterizedTest
	@ValueSource(strings = { "TO_DO", "IN_PROGRESS" })
	void aProjectWithAnUnfinishedTaskRemainsActive(String state) throws Exception {
		addTask("DONE");
		addTask(state);

		assertProblem(complete(this.anaToken), 409, "Conflict", UNFINISHED);

		assertThat(storedState()).isEqualTo("ACTIVE");
	}

	@Test
	void deletingTheLastUnfinishedTaskMakesTheProjectCompletable() throws Exception {
		addTask("DONE");
		String unfinished = addTask("TO_DO");
		assertProblem(complete(this.anaToken), 409, "Conflict", UNFINISHED);

		this.mvc.perform(withToken(delete(taskUrl(unfinished)), this.anaToken)).andExpect(status().isNoContent());

		complete(this.anaToken).andExpect(status().isOk());
		assertThat(storedState()).isEqualTo("COMPLETED");
	}

	@Test
	void onlyTheProjectOwnerCompletesAndANonMemberCannotSeeTheProject() throws Exception {
		assertProblem(complete(this.ruiToken), 403, "Forbidden", NOT_OWNER);
		assertProblem(complete(this.evaToken), 404, "Not Found", NOT_FOUND);
		assertProblem(completeAt(this.anaToken, PROJECTS + "/" + UUID.randomUUID() + "/state"), 404, "Not Found",
				NOT_FOUND);
		assertProblem(completeAt(this.anaToken, PROJECTS + "/not-a-uuid/state"), 404, "Not Found", NOT_FOUND);

		assertThat(storedState()).isEqualTo("ACTIVE");
	}

	@Test
	void aParticipantIsRefusedBeforeTheContentIsRead() throws Exception {
		assertProblem(send(this.ruiToken, MediaType.TEXT_PLAIN, "nonsense"), 403, "Forbidden", NOT_OWNER);
		assertProblem(send(this.evaToken, MediaType.APPLICATION_JSON, "{"), 404, "Not Found", NOT_FOUND);
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"state\":\"ACTIVE\"}", "{\"state\":\"completed\"}", "{\"state\":null}", "{}",
			"{\"state\":\"COMPLETED\",\"name\":\"x\"}", "[]", "\"COMPLETED\"", "{\"state\":1}" })
	void anyContentOtherThanTheStateCompletedGets422(String content) throws Exception {
		MvcResult result = assertProblem(send(this.anaToken, MediaType.APPLICATION_JSON, content), 422,
				"Unprocessable Content", ONLY_COMPLETED);

		Contract.assertSatisfies("ValidationProblem", result.getResponse().getContentAsString());
		assertThat(body(result).path("errors").size()).isPositive();
		assertThat(storedState()).isEqualTo("ACTIVE");
	}

	@Test
	void reopeningGets422WithThePointerOfTheState() throws Exception {
		JsonNode errors = body(assertProblem(send(this.anaToken, MediaType.APPLICATION_JSON, "{\"state\":\"ACTIVE\"}"),
				422, "Unprocessable Content", ONLY_COMPLETED)).path("errors");

		assertThat(errors.size()).isEqualTo(1);
		assertThat(errors.get(0).path("pointer").stringValue()).isEqualTo("/state");
		assertThat(errors.get(0).path("detail").stringValue()).isEqualTo("The only accepted value is COMPLETED.");
	}

	@Test
	void malformedContentGets400AndAnotherMediaTypeGets415() throws Exception {
		assertProblem(send(this.anaToken, MediaType.APPLICATION_JSON, "{\"state\":"), 400, "Bad Request",
				"The request content is not well-formed JSON.");
		assertProblem(send(this.anaToken, MediaType.TEXT_PLAIN, "{\"state\":\"COMPLETED\"}"), 415,
				"Unsupported Media Type", "The request content must be in the operation's media type.");
		assertProblem(send(this.anaToken, MERGE_PATCH, "{\"state\":\"COMPLETED\"}"), 415, "Unsupported Media Type",
				"The request content must be in the operation's media type.");

		assertThat(storedState()).isEqualTo("ACTIVE");
	}

	@Test
	void aRequestWithoutATokenIsRefused() throws Exception {
		TestAccess.assertNoToken(this.mvc.perform(put(PROJECTS + "/" + this.project + "/state")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"state\":\"COMPLETED\"}")));

		assertThat(storedState()).isEqualTo("ACTIVE");
	}

	// After completing --------------------------------------------------------------------------

	@ParameterizedTest
	@ValueSource(strings = { "editProject", "deleteProject", "createTask", "updateTask", "changeTaskState",
			"deleteTask", "assignTask", "unassignTask", "completeProject" })
	void aCompletedProjectRefusesEveryChangeAndChangesNothing(String operation) throws Exception {
		String task = addTask("DONE");
		assign(task, this.rui);
		complete(this.anaToken).andExpect(status().isOk());
		String before = snapshot();

		assertProblem(perform(operation, task), 409, "Conflict", COMPLETED);

		assertThat(snapshot()).isEqualTo(before);
	}

	@Test
	void anAssigneeOfACompletedProjectCannotChangeTheStateOrTheAssignees() throws Exception {
		String task = addTask("DONE");
		assign(task, this.rui);
		complete(this.anaToken).andExpect(status().isOk());
		String before = snapshot();

		assertProblem(changeTaskState(this.ruiToken, task, "IN_PROGRESS"), 409, "Conflict", COMPLETED);
		assertProblem(this.mvc.perform(withToken(delete(taskUrl(task) + "/assignees/" + this.rui.id()), this.ruiToken)),
				409, "Conflict", COMPLETED);
		assertProblem(this.mvc.perform(withToken(post(taskUrl(task) + "/assignees"), this.ruiToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"personId\":\"" + this.rui.id() + "\"}")), 409, "Conflict", COMPLETED);

		assertThat(snapshot()).isEqualTo(before);
	}

	@Test
	void aCompletedProjectStillAcceptsComments() throws Exception {
		String task = addTask("DONE");
		complete(this.anaToken).andExpect(status().isOk());

		for (String token : new String[] { this.anaToken, this.ruiToken }) {
			this.mvc.perform(withToken(post(taskUrl(task) + "/comments"), token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"text\":\"Wrapped up.\"}")).andExpect(status().isCreated());
		}
		assertThat(this.jdbc.sql("SELECT count(*) FROM comment WHERE task_id = ?")
			.param(UUID.fromString(task))
			.query(Integer.class)
			.single()).isEqualTo(2);
	}

	// Simultaneous changes (ADR-006; Assumption A-029) ------------------------------------------

	@Test
	void completingWhileATaskIsReopenedLeavesNoUnfinishedTaskInACompletedProject() throws Exception {
		for (int round = 0; round < 5; round++) {
			this.project = createProject("Reopened " + round);
			String task = addTask("DONE");

			List<Integer> statuses = simultaneously(() -> statusOf(complete(this.anaToken)),
					() -> statusOf(changeTaskState(this.anaToken, task, "TO_DO")));

			assertThat(statuses).as("round %d", round).containsAnyOf(200).containsAnyOf(409);
			assertConsistent();
		}
	}

	@Test
	void completingWhileATaskIsCreatedLeavesNoUnfinishedTaskInACompletedProject() throws Exception {
		for (int round = 0; round < 5; round++) {
			this.project = createProject("Created " + round);

			List<Integer> statuses = simultaneously(() -> statusOf(complete(this.anaToken)),
					() -> statusOf(createTask(this.anaToken)));

			assertThat(statuses).as("round %d", round).containsAnyOf(200, 201).contains(409);
			assertConsistent();
		}
	}

	@Test
	void completingTwiceAtOnceSucceedsExactlyOnce() throws Exception {
		addTask("DONE");

		List<Integer> statuses = simultaneously(() -> statusOf(complete(this.anaToken)),
				() -> statusOf(complete(this.anaToken)));

		assertThat(statuses).containsExactlyInAnyOrder(200, 409);
		assertThat(storedState()).isEqualTo("COMPLETED");
	}

	// Helpers --------------------------------------------------------------------------------

	private void assertConsistent() {
		Integer unfinished = this.jdbc.sql("""
				SELECT count(*) FROM task
				WHERE project_id = ? AND deleted_at IS NULL AND state <> 'DONE'
				""").param(UUID.fromString(this.project)).query(Integer.class).single();
		if ("COMPLETED".equals(storedState())) {
			assertThat(unfinished).as("unfinished tasks in a Completed project").isZero();
		}
		else {
			assertThat(unfinished).as("an Active project after a refused completion").isPositive();
		}
	}

	private ResultActions perform(String operation, String task) throws Exception {
		return switch (operation) {
			case "editProject" -> this.mvc.perform(withToken(patch(PROJECTS + "/" + this.project), this.anaToken)
				.contentType(MERGE_PATCH)
				.content("{\"name\":\"Renamed\"}"));
			case "deleteProject" -> this.mvc.perform(withToken(delete(PROJECTS + "/" + this.project), this.anaToken));
			case "createTask" -> createTask(this.anaToken);
			case "updateTask" -> this.mvc.perform(withToken(patch(taskUrl(task)), this.anaToken).contentType(MERGE_PATCH)
				.content("{\"title\":\"Renamed\"}"));
			case "changeTaskState" -> changeTaskState(this.anaToken, task, "TO_DO");
			case "deleteTask" -> this.mvc.perform(withToken(delete(taskUrl(task)), this.anaToken));
			case "assignTask" -> this.mvc.perform(withToken(post(taskUrl(task) + "/assignees"), this.anaToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"personId\":\"" + this.ana.id() + "\"}"));
			case "unassignTask" ->
				this.mvc.perform(withToken(delete(taskUrl(task) + "/assignees/" + this.rui.id()), this.anaToken));
			case "completeProject" -> complete(this.anaToken);
			default -> throw new IllegalArgumentException(operation);
		};
	}

	/** The rows a change could touch: the project, its tasks, and their assignments. */
	private String snapshot() {
		UUID id = UUID.fromString(this.project);
		return String.join("\n",
				this.jdbc.sql("SELECT name, description, state, deleted_at FROM project WHERE project_id = ?")
					.param(id)
					.query()
					.listOfRows()
					.toString(),
				this.jdbc.sql("""
						SELECT task_id, title, description, priority, state, deleted_at FROM task
						WHERE project_id = ? ORDER BY task_id
						""").param(id).query().listOfRows().toString(),
				this.jdbc.sql("SELECT task_id, person_id FROM assignment WHERE project_id = ? ORDER BY task_id, person_id")
					.param(id)
					.query()
					.listOfRows()
					.toString());
	}

	private ResultActions complete(String token) throws Exception {
		return send(token, MediaType.APPLICATION_JSON, "{\"state\":\"COMPLETED\"}");
	}

	private ResultActions completeAt(String token, String url) throws Exception {
		return this.mvc.perform(withToken(put(url), token).contentType(MediaType.APPLICATION_JSON)
			.content("{\"state\":\"COMPLETED\"}"));
	}

	private ResultActions send(String token, MediaType type, String content) throws Exception {
		return this.mvc.perform(withToken(put(PROJECTS + "/" + this.project + "/state"), token).contentType(type)
			.content(content));
	}

	private ResultActions createTask(String token) throws Exception {
		return this.mvc.perform(withToken(post(PROJECTS + "/" + this.project + "/tasks"), token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"Late task\"}"));
	}

	private ResultActions changeTaskState(String token, String task, String state) throws Exception {
		return this.mvc.perform(withToken(put(taskUrl(task) + "/state"), token).contentType(MediaType.APPLICATION_JSON)
			.content("{\"state\":\"" + state + "\"}"));
	}

	private String taskUrl(String task) {
		return PROJECTS + "/" + this.project + "/tasks/" + task;
	}

	private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
		return request.header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(token));
	}

	private String createProject(String name) throws Exception {
		return body(this.mvc.perform(withToken(post(PROJECTS), this.anaToken).contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"" + name + "\"}")).andExpect(status().isCreated()).andReturn()).path("projectId")
			.stringValue();
	}

	private String addTask(String state) {
		UUID task = UUID.randomUUID();
		this.jdbc.sql("INSERT INTO task (task_id, project_id, title, state) VALUES (?, ?, 'A task', ?)")
			.params(task, UUID.fromString(this.project), state)
			.update();
		return task.toString();
	}

	private void assign(String task, TestAccess.TestPerson person) {
		this.jdbc.sql("INSERT INTO assignment (task_id, person_id, project_id) VALUES (?, ?, ?)")
			.params(UUID.fromString(task), person.id(), UUID.fromString(this.project))
			.update();
	}

	private void addMember(TestAccess.TestPerson person) {
		this.jdbc.sql("INSERT INTO membership (project_id, person_id, role) VALUES (?, ?, 'PROJECT_PARTICIPANT')")
			.params(UUID.fromString(this.project), person.id())
			.update();
	}

	private String storedState() {
		return this.jdbc.sql("SELECT state FROM project WHERE project_id = ?")
			.param(UUID.fromString(this.project))
			.query(String.class)
			.single();
	}

	private JsonNode view(String token) throws Exception {
		MvcResult result = this.mvc.perform(withToken(get(PROJECTS + "/" + this.project), token))
			.andExpect(status().isOk())
			.andReturn();
		Contract.assertSatisfies("Project", result.getResponse().getContentAsString());
		return body(result);
	}

	private static JsonNode body(MvcResult result) throws Exception {
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString());
	}

}
