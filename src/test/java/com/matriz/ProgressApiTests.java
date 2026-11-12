package com.matriz;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A project's progress and each listed project's task counts through the API (openapi.yaml,
 * {@code getProject}, {@code listProjects}, Progress, TaskCounts; FR-012, FR-024; US-006, US-017,
 * US-025; EX-005, EX-026, EX-027; Assumptions A-006, A-022, A-025, A-026), with real persistence.
 */
@AutoConfigureMockMvc
class ProgressApiTests extends DatabaseTestSupport {

	private static final String PROJECTS = "/api/projects";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcClient jdbc;

	private TestAccess.TestPerson ana;

	private TestAccess.TestPerson rui;

	private String anaToken;

	private String ruiToken;

	@BeforeEach
	void addPeople() throws Exception {
		this.ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		this.rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		this.anaToken = TestAccess.accessToken(this.mvc, this.ana);
		this.ruiToken = TestAccess.accessToken(this.mvc, this.rui);
	}

	@Test
	void aProjectShowsItsCountsByStateAndItsPercentageDone() throws Exception {
		String project = createProject("Alpha");
		addTask(project, "TO_DO");
		addTask(project, "IN_PROGRESS");
		addTask(project, "DONE");
		addTask(project, "DONE");
		addMember(project, this.rui);

		for (String token : new String[] { this.anaToken, this.ruiToken }) {
			assertProgress(view(token, project).path("progress"), 1, 1, 2, 50);
		}
	}

	@Test
	void aProjectWithNoTasksHasZeroCountsAndANullPercentage() throws Exception {
		MvcResult created = createRequest("Empty");
		JsonNode body = JsonMapper.shared().readTree(created.getResponse().getContentAsString());
		String project = body.path("projectId").stringValue();

		assertProgress(body.path("progress"), 0, 0, 0, null);
		JsonNode progress = view(this.anaToken, project).path("progress");
		assertProgress(progress, 0, 0, 0, null);
		assertThat(progress.has("percentDone")).as("percentDone is written as null, not left out").isTrue();
	}

	@Test
	void oneDoneOfThreeIsThirtyThreePercent() throws Exception {
		String project = createProject("Rounding");
		addTask(project, "TO_DO");
		addTask(project, "IN_PROGRESS");
		addTask(project, "DONE");

		assertProgress(view(this.anaToken, project).path("progress"), 1, 1, 1, 33);
	}

	@Test
	void deletedTasksAreNotCountedAndDeletingAToDoTaskRaisesThePercentage() throws Exception {
		String project = createProject("US-017");
		String toDo = createTask(project, "To delete");
		changeState(project, createTask(project, "Working"), "IN_PROGRESS");
		changeState(project, createTask(project, "First done"), "DONE");
		changeState(project, createTask(project, "Second done"), "DONE");
		assertProgress(view(this.anaToken, project).path("progress"), 1, 1, 2, 50);

		this.mvc.perform(withToken(delete(PROJECTS + "/" + project + "/tasks/" + toDo), this.anaToken))
			.andExpect(status().isNoContent());

		assertProgress(view(this.anaToken, project).path("progress"), 0, 1, 2, 67);
		assertThat(countsInList(this.anaToken, project)).isEqualTo(new int[] { 0, 1, 2 });
	}

	@Test
	void creatingEditingAndChangingTheStateOfTasksUpdatesTheProgress() throws Exception {
		String project = createProject("Live");
		String task = createTask(project, "Only task");
		assertProgress(view(this.anaToken, project).path("progress"), 1, 0, 0, 0);

		changeState(project, task, "DONE");
		assertProgress(view(this.anaToken, project).path("progress"), 0, 0, 1, 100);

		this.mvc.perform(withToken(patch(PROJECTS + "/" + project + "/tasks/" + task), this.anaToken)
			.contentType(MediaType.parseMediaType("application/merge-patch+json"))
			.content("{\"title\":\"Renamed\"}")).andExpect(status().isOk());
		assertProgress(view(this.anaToken, project).path("progress"), 0, 0, 1, 100);
	}

	@Test
	void taskFiltersNeverChangeTheProgress() throws Exception {
		String project = createProject("Filters");
		addTask(project, "TO_DO");
		addTask(project, "DONE");

		JsonNode filtered = body(this.mvc.perform(withToken(get(PROJECTS + "/" + project + "/tasks?state=DONE"),
				this.anaToken)).andExpect(status().isOk()).andReturn());

		assertThat(filtered.path("totalItems").asInt()).isEqualTo(1);
		assertProgress(view(this.anaToken, project).path("progress"), 1, 0, 1, 50);
	}

	@Test
	void eachListedProjectHasItsOwnCountsAndNoTotalsAcrossProjects() throws Exception {
		String alpha = createProject("Alpha");
		addTask(alpha, "TO_DO");
		addTask(alpha, "TO_DO");
		addTask(alpha, "IN_PROGRESS");
		String beta = createProject("Beta");
		addTask(beta, "DONE");
		String empty = createProject("Gamma");
		String deleted = addTask(beta, "TO_DO");
		this.jdbc.sql("UPDATE task SET deleted_at = now() WHERE task_id = ?").param(UUID.fromString(deleted)).update();
		String others = createProjectAs(this.ruiToken, "Rui's");
		addTask(others, "DONE");

		JsonNode page = list(this.anaToken, "");

		assertThat(page.path("items").size()).isEqualTo(3);
		assertThat(countsInList(this.anaToken, alpha)).isEqualTo(new int[] { 2, 1, 0 });
		assertThat(countsInList(this.anaToken, beta)).isEqualTo(new int[] { 0, 0, 1 });
		assertThat(countsInList(this.anaToken, empty)).isEqualTo(new int[] { 0, 0, 0 });
		page.path("items").forEach((item) -> {
			assertThat(item.has("progress")).as("no percentage in the list").isFalse();
			assertThat(item.path("taskCounts").propertyNames()).containsExactlyInAnyOrder("toDo", "inProgress",
					"done");
		});
		assertThat(page.propertyNames()).containsExactlyInAnyOrder("items", "page", "size", "totalItems",
				"totalPages");
	}

	@Test
	void aCompletedProjectKeepsItsProgressAndCounts() throws Exception {
		String project = createProject("Finished");
		addTask(project, "DONE");
		addTask(project, "DONE");
		this.jdbc.sql("UPDATE project SET state = 'COMPLETED' WHERE project_id = ?")
			.param(UUID.fromString(project))
			.update();

		assertProgress(view(this.anaToken, project).path("progress"), 0, 0, 2, 100);
		assertThat(countsInList(this.anaToken, project)).isEqualTo(new int[] { 0, 0, 2 });
	}

	@Test
	void anEditedProjectReturnsItsProgress() throws Exception {
		String project = createProject("Before");
		addTask(project, "DONE");

		JsonNode edited = body(this.mvc.perform(withToken(patch(PROJECTS + "/" + project), this.anaToken)
			.contentType(MediaType.parseMediaType("application/merge-patch+json"))
			.content("{\"name\":\"After\"}")).andExpect(status().isOk()).andReturn());

		Contract.assertSatisfies("Project", edited.toString());
		assertProgress(edited.path("progress"), 0, 0, 1, 100);
	}

	// Helpers --------------------------------------------------------------------------------

	private static void assertProgress(JsonNode progress, int toDo, int inProgress, int done, Integer percent) {
		JsonNode counts = progress.path("taskCounts");
		assertThat(new int[] { counts.path("toDo").asInt(-1), counts.path("inProgress").asInt(-1),
				counts.path("done").asInt(-1) }).as("counts in %s", progress).isEqualTo(new int[] { toDo, inProgress, done });
		if (percent == null) {
			assertThat(progress.path("percentDone").isNull()).as("percentDone in %s", progress).isTrue();
		}
		else {
			assertThat(progress.path("percentDone").asInt(-1)).as("percentDone in %s", progress).isEqualTo(percent);
		}
	}

	private int[] countsInList(String token, String project) throws Exception {
		JsonNode item = list(token, "?size=100").path("items")
			.valueStream()
			.filter((each) -> project.equals(each.path("projectId").stringValue()))
			.findFirst()
			.orElseThrow();
		JsonNode counts = item.path("taskCounts");
		return new int[] { counts.path("toDo").asInt(-1), counts.path("inProgress").asInt(-1),
				counts.path("done").asInt(-1) };
	}

	private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
		return request.header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(token));
	}

	private MvcResult createRequest(String name) throws Exception {
		MvcResult result = this.mvc.perform(withToken(post(PROJECTS), this.anaToken).contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"" + name + "\"}")).andExpect(status().isCreated()).andReturn();
		Contract.assertSatisfies("Project", result.getResponse().getContentAsString());
		return result;
	}

	private String createProject(String name) throws Exception {
		return createProjectAs(this.anaToken, name);
	}

	private String createProjectAs(String token, String name) throws Exception {
		return body(this.mvc.perform(withToken(post(PROJECTS), token).contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"" + name + "\"}")).andExpect(status().isCreated()).andReturn()).path("projectId")
			.stringValue();
	}

	private String createTask(String project, String title) throws Exception {
		return body(this.mvc.perform(withToken(post(PROJECTS + "/" + project + "/tasks"), this.anaToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"" + title + "\"}")).andExpect(status().isCreated()).andReturn()).path("taskId")
			.stringValue();
	}

	private void changeState(String project, String task, String state) throws Exception {
		this.mvc.perform(withToken(put(PROJECTS + "/" + project + "/tasks/" + task + "/state"), this.anaToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"state\":\"" + state + "\"}")).andExpect(status().isOk());
	}

	private String addTask(String project, String state) {
		UUID task = UUID.randomUUID();
		this.jdbc.sql("INSERT INTO task (task_id, project_id, title, state) VALUES (?, ?, 'A task', ?)")
			.params(task, UUID.fromString(project), state)
			.update();
		return task.toString();
	}

	private void addMember(String project, TestAccess.TestPerson person) {
		this.jdbc.sql("INSERT INTO membership (project_id, person_id, role) VALUES (?, ?, 'PROJECT_PARTICIPANT')")
			.params(UUID.fromString(project), person.id())
			.update();
	}

	private JsonNode view(String token, String project) throws Exception {
		MvcResult result = this.mvc.perform(withToken(get(PROJECTS + "/" + project), token))
			.andExpect(status().isOk())
			.andReturn();
		Contract.assertSatisfies("Project", result.getResponse().getContentAsString());
		return body(result);
	}

	private JsonNode list(String token, String query) throws Exception {
		MvcResult result = this.mvc.perform(withToken(get(java.net.URI.create(PROJECTS + query)), token))
			.andExpect(status().isOk())
			.andReturn();
		Contract.assertSatisfies("ProjectPage", result.getResponse().getContentAsString());
		return body(result);
	}

	private static JsonNode body(MvcResult result) throws Exception {
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString());
	}

}
