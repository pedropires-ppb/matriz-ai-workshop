package com.matriz;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Assigning members to tasks through the API (openapi.yaml, {@code assignTask}, {@code unassignTask},
 * and {@code changeTaskState} for assignees; US-005, US-012, US-019, US-020; EX-004, EX-020 to EX-022,
 * EX-028 to EX-030; Assumptions A-019, A-020, A-035; ADR-004, ADR-006), with real persistence. Ana is
 * the Project Owner, Rui and Eva are Project Participants, and Daniel is a member of no project.
 */
@AutoConfigureMockMvc
class AssignmentApiTests extends DatabaseTestSupport {

	private static final String TASK_NOT_FOUND = "The project or task does not exist or is not accessible.";

	private static final String ASSIGNEE_NOT_FOUND = "The project, task, or assignee does not exist or is not accessible.";

	private static final String NOT_A_MEMBER = "Only members of the project can be assigned to its tasks.";

	private static final String ASSIGN_SELF_ONLY = "A Project Participant can assign only themselves.";

	private static final String UNASSIGN_SELF_ONLY = "A Project Participant can unassign only themselves.";

	private static final String STATE_FORBIDDEN = "Only the Project Owner or an assignee of the task can change its state.";

	private static final String COMPLETED = "The project is Completed; it cannot be changed.";

	private static final String INVALID = "The task is invalid.";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcClient jdbc;

	private TestAccess.TestPerson ana;

	private TestAccess.TestPerson rui;

	private TestAccess.TestPerson eva;

	private TestAccess.TestPerson daniel;

	private String anaToken;

	private String ruiToken;

	private String evaToken;

	private String danielToken;

	private String project;

	private String task;

	@BeforeEach
	void addPeopleAProjectAndATask() throws Exception {
		this.ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		this.rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		this.eva = TestAccess.addPerson(this.jdbc, "Eva Costa");
		this.daniel = TestAccess.addPerson(this.jdbc, "Daniel Reis");
		this.anaToken = TestAccess.accessToken(this.mvc, this.ana);
		this.ruiToken = TestAccess.accessToken(this.mvc, this.rui);
		this.evaToken = TestAccess.accessToken(this.mvc, this.eva);
		this.danielToken = TestAccess.accessToken(this.mvc, this.daniel);
		this.project = createProject(this.anaToken, "Website Redesign");
		addParticipant(this.project, this.rui);
		addParticipant(this.project, this.eva);
		this.task = createTask(this.project, "Prepare proposal");
	}

	// Assigning --------------------------------------------------------------------------------

	@Test
	void theOwnerAssignsSeveralMembersAndTheyStayWhenTheTaskIsOpenedAgain() throws Exception {
		JsonNode first = ok(assign(this.anaToken, this.rui.id().toString()));
		assertThat(names(first)).containsExactly("Rui Martins");

		ok(assign(this.anaToken, this.eva.id().toString()));
		JsonNode third = ok(assign(this.anaToken, this.ana.id().toString()));

		assertThat(names(third)).containsExactly("Ana Silva", "Eva Costa", "Rui Martins");
		assertThat(third.path("state").stringValue()).isEqualTo("TO_DO");
		for (String token : List.of(this.anaToken, this.ruiToken, this.evaToken)) {
			assertThat(names(ok(this.mvc.perform(authorized(get(taskUrl()), token)))))
				.containsExactly("Ana Silva", "Eva Costa", "Rui Martins");
		}
		JsonNode listed = JsonMapper.shared()
			.readTree(this.mvc.perform(authorized(get(tasksUrl()), this.ruiToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString());
		assertThat(names(listed.path("items").get(0))).containsExactly("Ana Silva", "Eva Costa", "Rui Martins");
		assertThat(count("SELECT count(*) FROM assignment WHERE task_id = ? AND project_id = ?", id(this.task),
				id(this.project))).isEqualTo(3);
	}

	@Test
	void assigningAPersonWhoIsAlreadyAnAssigneeChangesNothing() throws Exception {
		ok(assign(this.anaToken, this.rui.id().toString()));

		JsonNode again = ok(assign(this.anaToken, this.rui.id().toString()));

		assertThat(names(again)).containsExactly("Rui Martins");
		assertThat(count("SELECT count(*) FROM assignment WHERE task_id = ?", id(this.task))).isEqualTo(1);
	}

	@Test
	void twoRequestsToAssignTheSamePersonAtTheSameTimeLeaveOneAssignment() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			CountDownLatch ready = new CountDownLatch(2);
			CountDownLatch go = new CountDownLatch(1);
			List<Future<Integer>> results = new ArrayList<>();
			for (String token : List.of(this.anaToken, this.ruiToken)) {
				results.add(pool.submit(() -> {
					ready.countDown();
					go.await();
					return assign(token, this.rui.id().toString()).andReturn().getResponse().getStatus();
				}));
			}
			ready.await();
			go.countDown();
			for (Future<Integer> result : results) {
				assertThat(result.get()).isEqualTo(200);
			}
		}
		finally {
			pool.shutdownNow();
		}

		assertThat(count("SELECT count(*) FROM assignment WHERE task_id = ?", id(this.task))).isEqualTo(1);
	}

	@Test
	void aPersonWhoIsNotAMemberOrDoesNotExistCannotBeAssigned() throws Exception {
		String otherProject = createProject(this.danielToken, "Mobile App");
		assertThat(otherProject).isNotEqualTo(this.project);
		TestAccess.TestPerson outsider = TestAccess.addPerson(this.jdbc, "Outsider");

		for (String person : List.of(this.daniel.id().toString(), outsider.id().toString(),
				UUID.randomUUID().toString(), "not-an-identifier", "  ")) {
			assertNotAMember(assign(this.anaToken, person));
		}

		assertThat(count("SELECT count(*) FROM assignment WHERE task_id = ?", id(this.task))).isZero();
	}

	@Test
	void aMemberOfAnotherProjectButNotThisOneIsNotEligible() throws Exception {
		String mobileApp = createProject(this.anaToken, "Mobile App");
		assertThat(mobileApp).isNotEqualTo(this.project);

		assertThat(ok(assign(this.anaToken, this.eva.id().toString())).path("assignees")).hasSize(1);
		String otherTask = createTask(mobileApp, "Other");
		assertNotAMember(this.mvc.perform(authorized(post(tasksUrl(mobileApp) + "/" + otherTask + "/assignees"),
				this.anaToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"personId\":\"" + this.eva.id() + "\"}")));
	}

	@Test
	void theContentOfTheRequestIsCheckedAndTheAuthorOfAnyOtherPropertyIsRefused() throws Exception {
		for (String content : List.of("{}", "{\"personId\":1}", "{\"personId\":null}", "{\"personId\":\"\"}", "[]",
				"\"text\"", "{\"personId\":\"" + this.rui.id() + "\",\"role\":\"PROJECT_OWNER\"}")) {
			ResultActions actions = assignRaw(this.anaToken, MediaType.APPLICATION_JSON, content);
			MvcResult result = assertProblem(actions, 422, "Unprocessable Content", INVALID);
			Contract.assertSatisfies("ValidationProblem", result.getResponse().getContentAsString());
		}
		assertProblem(assignRaw(this.anaToken, MediaType.APPLICATION_JSON, "{"), 400, "Bad Request",
				"The request content is not well-formed JSON.");
		this.mvc.perform(authorized(post(assigneesUrl()), this.anaToken).contentType(MediaType.TEXT_PLAIN).content("x"))
			.andExpect(status().isUnsupportedMediaType());
		assertThat(count("SELECT count(*) FROM assignment WHERE task_id = ?", id(this.task))).isZero();
	}

	@Test
	void theChecksOfAssignTaskComeInTheOrderOfTheDecision() throws Exception {
		// 404 before 415, 400, and 422
		this.mvc.perform(authorized(post(assigneesUrl()), this.danielToken).contentType(MediaType.TEXT_PLAIN).content("x"))
			.andExpect(status().isNotFound());
		// 415 and 400 before 422 and before 403
		this.mvc.perform(authorized(post(assigneesUrl()), this.ruiToken).contentType(MediaType.TEXT_PLAIN).content("x"))
			.andExpect(status().isUnsupportedMediaType());
		assertProblem(assignRaw(this.ruiToken, MediaType.APPLICATION_JSON, "{"), 400, "Bad Request",
				"The request content is not well-formed JSON.");
		// 422 for the content before 403
		assertProblem(assignRaw(this.ruiToken, MediaType.APPLICATION_JSON, "{}"), 422, "Unprocessable Content",
				INVALID);
		// 403 before 422 for a person who is not a member, whether or not that person exists
		assertProblem(assign(this.ruiToken, this.daniel.id().toString()), 403, "Forbidden", ASSIGN_SELF_ONLY);
		assertProblem(assign(this.ruiToken, UUID.randomUUID().toString()), 403, "Forbidden", ASSIGN_SELF_ONLY);
		// 422 for a person who is not a member before 409
		complete();
		assertNotAMember(assign(this.anaToken, this.daniel.id().toString()));
		// 403 before 409
		assertProblem(assign(this.ruiToken, this.eva.id().toString()), 403, "Forbidden", ASSIGN_SELF_ONLY);
		assertProblem(assign(this.anaToken, this.rui.id().toString()), 409, "Conflict", COMPLETED);
	}

	@Test
	void aProjectParticipantAssignsOnlyThemselvesAndThenChangesTheState() throws Exception {
		assertProblem(assign(this.ruiToken, this.eva.id().toString()), 403, "Forbidden", ASSIGN_SELF_ONLY);
		assertProblem(changeState(this.ruiToken, "DONE"), 403, "Forbidden", STATE_FORBIDDEN);

		JsonNode assigned = ok(assign(this.ruiToken, this.rui.id().toString()));
		JsonNode changed = ok(changeState(this.ruiToken, "DONE"));

		assertThat(names(assigned)).containsExactly("Rui Martins");
		assertThat(changed.path("state").stringValue()).isEqualTo("DONE");
		assertThat(names(changed)).containsExactly("Rui Martins");
		assertProblem(changeState(this.evaToken, "IN_PROGRESS"), 403, "Forbidden", STATE_FORBIDDEN);
		assertThat(count("SELECT count(*) FROM assignment WHERE task_id = ?", id(this.task))).isEqualTo(1);
	}

	@Test
	void everyAssigneeAndTheOwnerChangeTheStateAndNobodyElseDoes() throws Exception {
		ok(assign(this.anaToken, this.rui.id().toString()));
		ok(assign(this.anaToken, this.eva.id().toString()));

		assertThat(ok(changeState(this.ruiToken, "IN_PROGRESS")).path("state").stringValue()).isEqualTo("IN_PROGRESS");
		assertThat(ok(changeState(this.evaToken, "DONE")).path("state").stringValue()).isEqualTo("DONE");
		assertThat(ok(changeState(this.anaToken, "TO_DO")).path("state").stringValue()).isEqualTo("TO_DO");
		assertProblem(changeState(this.danielToken, "DONE"), 404, "Not Found", TASK_NOT_FOUND);
	}

	@Test
	void aProjectParticipantCannotCreateEditOrDeleteATaskEvenWhenAssigned() throws Exception {
		ok(assign(this.anaToken, this.rui.id().toString()));

		this.mvc.perform(authorized(delete(taskUrl()), this.ruiToken)).andExpect(status().isForbidden());
		this.mvc.perform(authorized(patch(taskUrl()), this.ruiToken)
			.contentType(MediaType.parseMediaType("application/merge-patch+json"))
			.content("{\"title\":\"Mine\"}")).andExpect(status().isForbidden());
		this.mvc.perform(authorized(post(tasksUrl()), this.ruiToken).contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"Mine\"}")).andExpect(status().isForbidden());
	}

	@Test
	void aRequestFromANonMemberOrWithoutAValidTokenOrForAnUnknownTaskIsRefused() throws Exception {
		assertProblem(assign(this.danielToken, this.rui.id().toString()), 404, "Not Found", TASK_NOT_FOUND);
		assertProblem(unassign(this.danielToken, this.rui.id().toString()), 404, "Not Found", TASK_NOT_FOUND);
		TestAccess.assertNoToken(this.mvc.perform(post(assigneesUrl()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"personId\":\"" + this.rui.id() + "\"}")));
		TestAccess.assertNoToken(this.mvc.perform(delete(assigneesUrl() + "/" + this.rui.id())));
		TestAccess.assertInvalidToken(this.mvc.perform(authorized(post(assigneesUrl()), "garbage")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"personId\":\"" + this.rui.id() + "\"}")));
		for (String id : List.of(UUID.randomUUID().toString(), "nope")) {
			assertProblem(this.mvc.perform(authorized(post(tasksUrl() + "/" + id + "/assignees"), this.anaToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"personId\":\"" + this.rui.id() + "\"}")), 404, "Not Found", TASK_NOT_FOUND);
		}
		this.mvc.perform(authorized(delete(assigneesUrl()), this.anaToken)).andExpect(status().isMethodNotAllowed());
	}

	@Test
	void aTaskOfAnotherProjectIsNotFoundThroughThisOne() throws Exception {
		String mobileApp = createProject(this.anaToken, "Mobile App");
		String other = createTask(mobileApp, "Other");

		assertProblem(this.mvc.perform(authorized(post(tasksUrl() + "/" + other + "/assignees"), this.anaToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"personId\":\"" + this.ana.id() + "\"}")), 404, "Not Found", TASK_NOT_FOUND);
	}

	@Test
	void aDeletedTaskCannotBeAssignedOrUnassignedAndItsAssignmentsAreRemoved() throws Exception {
		ok(assign(this.anaToken, this.rui.id().toString()));
		this.mvc.perform(authorized(delete(taskUrl()), this.anaToken)).andExpect(status().isNoContent());

		assertProblem(assign(this.anaToken, this.eva.id().toString()), 404, "Not Found", TASK_NOT_FOUND);
		assertProblem(unassign(this.anaToken, this.rui.id().toString()), 404, "Not Found", TASK_NOT_FOUND);
		assertThat(count("SELECT count(*) FROM assignment WHERE task_id = ?", id(this.task))).isZero();
	}

	// Unassigning ------------------------------------------------------------------------------

	@Test
	void theOwnerRemovesEachAssignmentAndTheLastOneCanBeRemoved() throws Exception {
		ok(assign(this.anaToken, this.rui.id().toString()));
		ok(assign(this.anaToken, this.eva.id().toString()));
		ok(changeState(this.anaToken, "IN_PROGRESS"));

		JsonNode afterRui = ok(unassign(this.anaToken, this.rui.id().toString()));
		assertThat(names(afterRui)).containsExactly("Eva Costa");
		assertThat(afterRui.path("state").stringValue()).isEqualTo("IN_PROGRESS");

		JsonNode afterEva = ok(unassign(this.anaToken, this.eva.id().toString()));
		assertThat(afterEva.path("assignees")).isEmpty();
		assertThat(afterEva.path("state").stringValue()).isEqualTo("IN_PROGRESS");
		assertThat(count("SELECT count(*) FROM assignment WHERE task_id = ?", id(this.task))).isZero();
	}

	@Test
	void aProjectParticipantRemovesOnlyTheirOwnAssignment() throws Exception {
		ok(assign(this.anaToken, this.rui.id().toString()));
		ok(assign(this.anaToken, this.eva.id().toString()));

		assertProblem(unassign(this.ruiToken, this.eva.id().toString()), 403, "Forbidden", UNASSIGN_SELF_ONLY);
		assertThat(names(ok(unassign(this.ruiToken, this.rui.id().toString())))).containsExactly("Eva Costa");

		assertProblem(changeState(this.ruiToken, "DONE"), 403, "Forbidden", STATE_FORBIDDEN);
	}

	@Test
	void removingAPersonWhoIsNotAnAssigneeIsNotFound() throws Exception {
		ok(assign(this.anaToken, this.rui.id().toString()));

		for (String person : List.of(this.eva.id().toString(), this.daniel.id().toString(),
				UUID.randomUUID().toString(), "nope")) {
			assertProblem(unassign(this.anaToken, person), 404, "Not Found", ASSIGNEE_NOT_FOUND);
		}
		// 404 before 403
		assertProblem(unassign(this.ruiToken, this.eva.id().toString()), 404, "Not Found", ASSIGNEE_NOT_FOUND);
		assertThat(count("SELECT count(*) FROM assignment WHERE task_id = ?", id(this.task))).isEqualTo(1);
	}

	@Test
	void aCompletedProjectRefusesToChangeItsAssignees() throws Exception {
		ok(assign(this.anaToken, this.rui.id().toString()));
		complete();

		assertProblem(assign(this.anaToken, this.eva.id().toString()), 409, "Conflict", COMPLETED);
		assertProblem(assign(this.ruiToken, this.rui.id().toString()), 409, "Conflict", COMPLETED);
		assertProblem(unassign(this.anaToken, this.rui.id().toString()), 409, "Conflict", COMPLETED);
		assertProblem(unassign(this.ruiToken, this.rui.id().toString()), 409, "Conflict", COMPLETED);
		assertProblem(changeState(this.ruiToken, "DONE"), 409, "Conflict", COMPLETED);
		assertProblem(unassign(this.ruiToken, this.eva.id().toString()), 404, "Not Found", ASSIGNEE_NOT_FOUND);
		assertThat(count("SELECT count(*) FROM assignment WHERE task_id = ?", id(this.task))).isEqualTo(1);
	}

	@ParameterizedTest
	@ValueSource(strings = { "PUT", "PATCH", "GET" })
	void otherMethodsOnTheAssigneeCollectionAreNotAllowed(String method) throws Exception {
		this.mvc.perform(authorized(request(HttpMethod.valueOf(method), assigneesUrl()), this.anaToken))
			.andExpect(status().isMethodNotAllowed());
	}

	// Database ---------------------------------------------------------------------------------

	@Test
	void theDatabaseRefusesATaskAssignedTwiceToTheSamePersonAndAnAssigneeWhoIsNotAMember() {
		this.jdbc.sql("INSERT INTO assignment (task_id, person_id, project_id) VALUES (?, ?, ?)")
			.params(id(this.task), this.rui.id(), id(this.project))
			.update();

		assertThatThrownBy(() -> this.jdbc.sql("INSERT INTO assignment (task_id, person_id, project_id) VALUES (?, ?, ?)")
			.params(id(this.task), this.rui.id(), id(this.project))
			.update()).isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> this.jdbc.sql("INSERT INTO assignment (task_id, person_id, project_id) VALUES (?, ?, ?)")
			.params(id(this.task), this.daniel.id(), id(this.project))
			.update()).isInstanceOf(DataIntegrityViolationException.class);
	}

	// Helpers ----------------------------------------------------------------------------------

	private String assigneesUrl() {
		return taskUrl() + "/assignees";
	}

	private String taskUrl() {
		return tasksUrl() + "/" + this.task;
	}

	private String tasksUrl() {
		return tasksUrl(this.project);
	}

	private static String tasksUrl(String project) {
		return "/api/projects/" + project + "/tasks";
	}

	private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, String token) {
		return request.header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(token));
	}

	private ResultActions assign(String token, String person) throws Exception {
		return assignRaw(token, MediaType.APPLICATION_JSON, "{\"personId\":\"" + person + "\"}");
	}

	private ResultActions assignRaw(String token, MediaType type, String content) throws Exception {
		return this.mvc.perform(authorized(post(assigneesUrl()), token).contentType(type).content(content));
	}

	private ResultActions unassign(String token, String person) throws Exception {
		return this.mvc.perform(authorized(delete(assigneesUrl() + "/" + person), token));
	}

	private ResultActions changeState(String token, String state) throws Exception {
		return this.mvc.perform(authorized(put(taskUrl() + "/state"), token).contentType(MediaType.APPLICATION_JSON)
			.content("{\"state\":\"" + state + "\"}"));
	}

	private JsonNode ok(ResultActions actions) throws Exception {
		MvcResult result = actions.andExpect(status().isOk()).andReturn();
		Contract.assertSatisfies("Task", result.getResponse().getContentAsString());
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString());
	}

	private void assertNotAMember(ResultActions actions) throws Exception {
		MvcResult result = assertProblem(actions, 422, "Unprocessable Content", NOT_A_MEMBER);
		Contract.assertSatisfies("ValidationProblem", result.getResponse().getContentAsString());
		JsonNode errors = JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("errors");
		assertThat(errors).hasSize(1);
		assertThat(errors.get(0).path("pointer").stringValue()).isEqualTo("/personId");
		assertThat(errors.get(0).path("detail").stringValue()).isEqualTo("This person is not a member of the project.");
	}

	private static List<String> names(JsonNode task) {
		return task.path("assignees").valueStream().map((person) -> person.path("displayName").stringValue()).toList();
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
			.perform(authorized(post(tasksUrl(project)), this.anaToken).contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"" + title + "\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("taskId").stringValue();
	}

	private void addParticipant(String project, TestAccess.TestPerson person) {
		this.jdbc.sql("INSERT INTO membership (project_id, person_id, role) VALUES (?, ?, 'PROJECT_PARTICIPANT')")
			.params(id(project), person.id())
			.update();
	}

	private void complete() {
		this.jdbc.sql("UPDATE project SET state = 'COMPLETED' WHERE project_id = ?").param(id(this.project)).update();
	}

	private static UUID id(String text) {
		return UUID.fromString(text);
	}

	private int count(String sql, Object... parameters) {
		return this.jdbc.sql(sql).params(parameters).query(Integer.class).single();
	}

}
