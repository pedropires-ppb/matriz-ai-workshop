package com.matriz;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.matriz.Contract.assertProblem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Adding, removing, and finding Project Participants through the API (openapi.yaml,
 * {@code addParticipant}, {@code removeParticipant}, {@code listEligibleParticipants}, and
 * {@code Member.hasAssignedTasks}; FR-004, FR-005; US-002, US-009; EX-007; Assumptions A-009, A-011,
 * A-029, A-030; ADR-006), with real persistence. The I6 decisions in the implementation plan. Ana is the
 * Project Owner, Rui a Project Participant, and Daniel and Eva members of no project. Other tests share
 * the database, so the people have a name unique to each test.
 */
@AutoConfigureMockMvc
class ParticipantsApiTests extends DatabaseTestSupport {

	private static final String PROJECTS = "/api/projects";

	private static final String NOT_FOUND = "The project does not exist or is not accessible.";

	private static final String MEMBER_NOT_FOUND = "The project or member does not exist or is not accessible.";

	private static final String NOT_OWNER = "Only the Project Owner of this project can do this.";

	private static final String COMPLETED = "The project is Completed; it cannot be changed.";

	private static final String ALREADY_MEMBER = "The person is already a member of the project.";

	private static final String OWNER = "The Project Owner cannot be removed from the project.";

	private static final String ASSIGNED = "The person is assigned to tasks of this project; remove those assignments first.";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcClient jdbc;

	private String unique;

	private TestAccess.TestPerson ana;

	private TestAccess.TestPerson rui;

	private TestAccess.TestPerson daniel;

	private TestAccess.TestPerson eva;

	private String anaToken;

	private String ruiToken;

	private String danielToken;

	private String project;

	@BeforeEach
	void addPeopleAndAProject() throws Exception {
		this.unique = "Q" + UUID.randomUUID().toString().substring(0, 8);
		this.ana = TestAccess.addPerson(this.jdbc, "Ana Silva " + this.unique);
		this.rui = TestAccess.addPerson(this.jdbc, "Rui Martins " + this.unique);
		this.daniel = TestAccess.addPerson(this.jdbc, "daniel Rocha " + this.unique);
		this.eva = TestAccess.addPerson(this.jdbc, "Eva Santos " + this.unique);
		this.anaToken = TestAccess.accessToken(this.mvc, this.ana);
		this.ruiToken = TestAccess.accessToken(this.mvc, this.rui);
		this.danielToken = TestAccess.accessToken(this.mvc, this.daniel);
		this.project = createProject("Alpha");
		add(this.anaToken, this.rui.id().toString()).andExpect(status().isCreated());
	}

	// Adding (US-002) ----------------------------------------------------------------------------

	@Test
	void theProjectOwnerAddsAPersonWhoThenSeesTheProject() throws Exception {
		assertProblem(view(this.danielToken), 404, "Not Found", NOT_FOUND);

		MvcResult result = add(this.anaToken, this.daniel.id().toString()).andExpect(status().isCreated())
			.andReturn();

		String body = result.getResponse().getContentAsString();
		Contract.assertSatisfies("Member", body);
		assertThat(body(result).path("person").path("personId").stringValue()).isEqualTo(this.daniel.id().toString());
		assertThat(body(result).path("person").path("displayName").stringValue())
			.isEqualTo("daniel Rocha " + this.unique);
		assertThat(body(result).path("role").stringValue()).isEqualTo("PROJECT_PARTICIPANT");
		assertThat(body(result).path("hasAssignedTasks").booleanValue()).isFalse();
		assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
			.isEqualTo(PROJECTS + "/" + this.project + "/members/" + this.daniel.id());
		assertThat(body).doesNotContain(this.daniel.loginName());
		view(this.danielToken).andExpect(status().isOk());
		assertThat(role(this.daniel)).isEqualTo("PROJECT_PARTICIPANT");
	}

	@Test
	void aMemberCannotBeAddedAgain() throws Exception {
		assertProblem(add(this.anaToken, this.rui.id().toString()), 409, "Conflict", ALREADY_MEMBER);
		assertProblem(add(this.anaToken, this.ana.id().toString()), 409, "Conflict", ALREADY_MEMBER);

		assertThat(role(this.ana)).isEqualTo("PROJECT_OWNER");
		assertThat(memberCount()).isEqualTo(2);
	}

	@Test
	void anUnknownPersonGets422WithThePointerOfThePerson() throws Exception {
		for (String unknown : List.of(UUID.randomUUID().toString(), "not-a-uuid", "1-2-3-4-5")) {
			MvcResult result = assertProblem(add(this.anaToken, unknown), 422, "Unprocessable Content",
					"The person to add was not found.");

			Contract.assertSatisfies("ValidationProblem", result.getResponse().getContentAsString());
			JsonNode errors = body(result).path("errors");
			assertThat(errors.size()).isEqualTo(1);
			assertThat(errors.get(0).path("pointer").stringValue()).isEqualTo("/personId");
			assertThat(errors.get(0).path("detail").stringValue()).isEqualTo("No person has this identifier.");
		}
		assertThat(memberCount()).isEqualTo(2);
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"personId\":\"\"}", "{\"personId\":1}", "{\"personId\":null}", "[]",
			"{\"personId\":\"x\",\"role\":\"PROJECT_OWNER\"}" })
	void contentOtherThanAPersonIdentifierGets422(String content) throws Exception {
		MvcResult result = assertProblem(send(this.anaToken, MediaType.APPLICATION_JSON, content), 422,
				"Unprocessable Content", "The person to add is required.");

		Contract.assertSatisfies("ValidationProblem", result.getResponse().getContentAsString());
		assertThat(memberCount()).isEqualTo(2);
	}

	@Test
	void malformedContentGets400AndAnotherMediaTypeGets415() throws Exception {
		assertProblem(send(this.anaToken, MediaType.APPLICATION_JSON, "{\"personId\":"), 400, "Bad Request",
				"The request content is not well-formed JSON.");
		assertProblem(send(this.anaToken, MediaType.TEXT_PLAIN, "{}"), 415, "Unsupported Media Type",
				"The request content must be in the operation's media type.");
	}

	@Test
	void aParticipantCannotAddAnyoneAndANonMemberCannotSeeTheProject() throws Exception {
		assertProblem(add(this.ruiToken, this.daniel.id().toString()), 403, "Forbidden", NOT_OWNER);
		assertProblem(send(this.ruiToken, MediaType.TEXT_PLAIN, "nonsense"), 403, "Forbidden", NOT_OWNER);
		assertProblem(add(this.danielToken, this.daniel.id().toString()), 404, "Not Found", NOT_FOUND);
		assertProblem(this.mvc.perform(withToken(post(PROJECTS + "/" + UUID.randomUUID() + "/members"), this.anaToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"personId\":\"" + this.daniel.id() + "\"}")), 404, "Not Found", NOT_FOUND);

		assertThat(memberCount()).isEqualTo(2);
	}

	// Removing (US-009, EX-007) --------------------------------------------------------------------

	@Test
	void theProjectOwnerRemovesAParticipantWhoImmediatelyLosesAccess() throws Exception {
		view(this.ruiToken).andExpect(status().isOk());

		remove(this.anaToken, this.rui.id().toString()).andExpect(status().isNoContent());

		assertProblem(view(this.ruiToken), 404, "Not Found", NOT_FOUND);
		assertThat(role(this.rui)).isNull();
		assertThat(memberNames(this.anaToken)).containsExactly("Ana Silva " + this.unique);
	}

	@Test
	void theProjectOwnerCannotBeRemoved() throws Exception {
		assertProblem(remove(this.anaToken, this.ana.id().toString()), 409, "Conflict", OWNER);

		assertThat(role(this.ana)).isEqualTo("PROJECT_OWNER");
	}

	@ParameterizedTest
	@ValueSource(strings = { "TO_DO", "IN_PROGRESS", "DONE" })
	void aMemberAssignedToATaskInAnyStateCannotBeRemoved(String state) throws Exception {
		String task = addTask(state);
		assign(task, this.rui);

		assertThat(member(this.ruiToken, this.rui).path("hasAssignedTasks").booleanValue()).isTrue();
		assertProblem(remove(this.anaToken, this.rui.id().toString()), 409, "Conflict", ASSIGNED);

		assertThat(role(this.rui)).isEqualTo("PROJECT_PARTICIPANT");
		view(this.ruiToken).andExpect(status().isOk());
	}

	@Test
	void unassigningOrDeletingTheTaskMakesTheMemberRemovable() throws Exception {
		String unassigned = addTask("TO_DO");
		String deleted = addTask("DONE");
		assign(unassigned, this.rui);
		assign(deleted, this.rui);

		this.mvc.perform(withToken(delete(taskUrl(unassigned) + "/assignees/" + this.rui.id()), this.anaToken))
			.andExpect(status().isOk());
		assertThat(member(this.anaToken, this.rui).path("hasAssignedTasks").booleanValue()).isTrue();
		this.mvc.perform(withToken(delete(taskUrl(deleted)), this.anaToken)).andExpect(status().isNoContent());
		assertThat(member(this.anaToken, this.rui).path("hasAssignedTasks").booleanValue()).isFalse();

		remove(this.anaToken, this.rui.id().toString()).andExpect(status().isNoContent());
		assertThat(role(this.rui)).isNull();
	}

	@Test
	void theRemovedPersonsCommentsAreKept() throws Exception {
		String task = addTask("TO_DO");
		this.mvc.perform(withToken(post(taskUrl(task) + "/comments"), this.ruiToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"text\":\"Rui was here\"}")).andExpect(status().isCreated());

		remove(this.anaToken, this.rui.id().toString()).andExpect(status().isNoContent());

		JsonNode comments = body(this.mvc.perform(withToken(get(taskUrl(task) + "/comments"), this.anaToken))
			.andExpect(status().isOk())
			.andReturn());
		assertThat(comments.get(0).path("author").path("displayName").stringValue())
			.isEqualTo("Rui Martins " + this.unique);
	}

	@Test
	void aPersonWhoIsNotAMemberOrAMalformedIdentifierIsNotFoundBeforeTheOwnerCheck() throws Exception {
		assertProblem(remove(this.anaToken, this.daniel.id().toString()), 404, "Not Found", MEMBER_NOT_FOUND);
		assertProblem(remove(this.anaToken, "not-a-uuid"), 404, "Not Found", MEMBER_NOT_FOUND);
		assertProblem(remove(this.ruiToken, this.daniel.id().toString()), 404, "Not Found", MEMBER_NOT_FOUND);
		assertProblem(remove(this.danielToken, this.rui.id().toString()), 404, "Not Found", MEMBER_NOT_FOUND);
		assertProblem(this.mvc.perform(withToken(delete(PROJECTS + "/not-a-uuid/members/" + this.rui.id()),
				this.anaToken)), 404, "Not Found", MEMBER_NOT_FOUND);
	}

	@Test
	void aParticipantCannotRemoveAnyoneIncludingThemselves() throws Exception {
		assertProblem(remove(this.ruiToken, this.rui.id().toString()), 403, "Forbidden", NOT_OWNER);
		assertProblem(remove(this.ruiToken, this.ana.id().toString()), 403, "Forbidden", NOT_OWNER);

		assertThat(memberCount()).isEqualTo(2);
	}

	// Completed projects (A-011) -------------------------------------------------------------------

	@Test
	void aCompletedProjectRefusesAddingAndRemovingButStillListsWhoCouldBeAdded() throws Exception {
		complete();

		assertProblem(add(this.anaToken, this.daniel.id().toString()), 409, "Conflict", COMPLETED);
		assertProblem(remove(this.anaToken, this.rui.id().toString()), 409, "Conflict", COMPLETED);
		assertProblem(add(this.anaToken, UUID.randomUUID().toString()), 422, "Unprocessable Content",
				"The person to add was not found.");
		assertThat(eligibleNames(this.anaToken, this.unique)).contains("daniel Rocha " + this.unique);

		assertThat(memberCount()).isEqualTo(2);
	}

	// Finding (A-030) ------------------------------------------------------------------------------

	@Test
	void theProjectOwnerSeesThePeopleWhoAreNotMembersByNameIgnoringLetterCase() throws Exception {
		MvcResult result = eligible(this.anaToken, null).andExpect(status().isOk()).andReturn();

		JsonNode people = body(result);
		for (JsonNode person : people) {
			Contract.assertSatisfies("PersonSummary", person.toString());
		}
		List<String> names = new ArrayList<>();
		people.forEach((person) -> names.add(person.path("displayName").stringValue()));
		assertThat(names).contains("daniel Rocha " + this.unique, "Eva Santos " + this.unique)
			.doesNotContain("Ana Silva " + this.unique, "Rui Martins " + this.unique);
		List<String> lowerCased = names.stream().map((name) -> name.toLowerCase(Locale.ROOT)).toList();
		assertThat(lowerCased).isSortedAccordingTo(Collator.getInstance(Locale.ROOT)::compare);
		assertThat(result.getResponse().getContentAsString()).doesNotContain(this.daniel.loginName(), "loginName");
	}

	@Test
	void theSearchIsTrimmedAndIgnoresLetterCaseButNotAccentsOrWildcards() throws Exception {
		TestAccess.addPerson(this.jdbc, "Débora Lima " + this.unique);

		assertThat(eligibleNames(this.anaToken, "  ROCHA " + this.unique.toLowerCase(Locale.ROOT) + " "))
			.containsExactly("daniel Rocha " + this.unique);
		assertThat(eligibleNames(this.anaToken, this.unique)).containsExactly("daniel Rocha " + this.unique,
				"Débora Lima " + this.unique, "Eva Santos " + this.unique);
		assertThat(eligibleNames(this.anaToken, "debora lima " + this.unique)).isEmpty();
		assertThat(eligibleNames(this.anaToken, "%" + this.unique)).isEmpty();
		assertThat(eligibleNames(this.anaToken, "_" + this.unique)).isEmpty();
	}

	@Test
	void anEmptySearchGets400() throws Exception {
		assertProblem(eligible(this.anaToken, ""), 400, "Bad Request", "The query parameter name must not be empty.");
		assertProblem(eligible(this.anaToken, "   "), 400, "Bad Request",
				"The query parameter name must not be empty.");
	}

	@Test
	void onlyTheProjectOwnerSeesWhoCanBeAdded() throws Exception {
		assertProblem(eligible(this.ruiToken, null), 403, "Forbidden", NOT_OWNER);
		assertProblem(eligible(this.danielToken, null), 404, "Not Found", NOT_FOUND);
	}

	@Test
	void requestsWithoutATokenAreRefused() throws Exception {
		TestAccess.assertNoToken(this.mvc.perform(get(PROJECTS + "/" + this.project + "/eligible-participants")));
		TestAccess.assertNoToken(this.mvc.perform(delete(PROJECTS + "/" + this.project + "/members/" + this.rui.id())));
		TestAccess.assertNoToken(this.mvc.perform(post(PROJECTS + "/" + this.project + "/members")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"personId\":\"" + this.daniel.id() + "\"}")));

		assertThat(memberCount()).isEqualTo(2);
	}

	// Simultaneous changes (ADR-006; Assumption A-029) ------------------------------------------

	@Test
	void removingAParticipantWhileTheyAssignThemselvesLeavesNoAssignmentWithoutItsMember() throws Exception {
		for (int round = 0; round < 5; round++) {
			String task = addTask("TO_DO");

			List<Integer> statuses = simultaneously(() -> statusOf(remove(this.anaToken, this.rui.id().toString())),
					() -> statusOf(this.mvc.perform(withToken(post(taskUrl(task) + "/assignees"), this.ruiToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"personId\":\"" + this.rui.id() + "\"}"))));

			Integer assignments = this.jdbc.sql("SELECT count(*) FROM assignment WHERE task_id = ? AND person_id = ?")
				.params(UUID.fromString(task), this.rui.id())
				.query(Integer.class)
				.single();
			if (role(this.rui) == null) {
				assertThat(statuses).as("round %d", round).containsExactlyInAnyOrder(204, 404);
				assertThat(assignments).as("round %d", round).isZero();
			}
			else {
				assertThat(statuses).as("round %d", round).containsExactlyInAnyOrder(409, 200);
				assertThat(assignments).as("round %d", round).isEqualTo(1);
			}
			this.jdbc.sql("DELETE FROM assignment WHERE task_id = ?").param(UUID.fromString(task)).update();
			if (role(this.rui) == null) {
				add(this.anaToken, this.rui.id().toString()).andExpect(status().isCreated());
			}
		}
	}

	// Helpers --------------------------------------------------------------------------------

	private ResultActions add(String token, String person) throws Exception {
		return send(token, MediaType.APPLICATION_JSON, "{\"personId\":\"" + person + "\"}");
	}

	private ResultActions send(String token, MediaType type, String content) throws Exception {
		return this.mvc.perform(withToken(post(PROJECTS + "/" + this.project + "/members"), token).contentType(type)
			.content(content));
	}

	private ResultActions remove(String token, String person) throws Exception {
		return this.mvc.perform(withToken(delete(PROJECTS + "/" + this.project + "/members/" + person), token));
	}

	private ResultActions eligible(String token, String name) throws Exception {
		MockHttpServletRequestBuilder request = get(PROJECTS + "/" + this.project + "/eligible-participants");
		if (name != null) {
			request.queryParam("name", name);
		}
		return this.mvc.perform(withToken(request, token));
	}

	private List<String> eligibleNames(String token, String name) throws Exception {
		List<String> names = new ArrayList<>();
		body(eligible(token, name).andExpect(status().isOk()).andReturn())
			.forEach((person) -> names.add(person.path("displayName").stringValue()));
		return names;
	}

	private ResultActions view(String token) throws Exception {
		return this.mvc.perform(withToken(get(PROJECTS + "/" + this.project), token));
	}

	private JsonNode member(String token, TestAccess.TestPerson person) throws Exception {
		MvcResult result = this.mvc.perform(withToken(get(PROJECTS + "/" + this.project + "/members"), token))
			.andExpect(status().isOk())
			.andReturn();
		for (JsonNode member : body(result)) {
			Contract.assertSatisfies("Member", member.toString());
			if (member.path("person").path("personId").stringValue().equals(person.id().toString())) {
				return member;
			}
		}
		throw new AssertionError("Not a member: " + person);
	}

	private List<String> memberNames(String token) throws Exception {
		List<String> names = new ArrayList<>();
		body(this.mvc.perform(withToken(get(PROJECTS + "/" + this.project + "/members"), token))
			.andExpect(status().isOk())
			.andReturn()).forEach((member) -> names.add(member.path("person").path("displayName").stringValue()));
		return names;
	}

	private void complete() throws Exception {
		this.mvc.perform(withToken(put(PROJECTS + "/" + this.project + "/state"), this.anaToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"state\":\"COMPLETED\"}")).andExpect(status().isOk());
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

	private String taskUrl(String task) {
		return PROJECTS + "/" + this.project + "/tasks/" + task;
	}

	private String role(TestAccess.TestPerson person) {
		return this.jdbc.sql("SELECT role FROM membership WHERE project_id = ? AND person_id = ?")
			.params(UUID.fromString(this.project), person.id())
			.query(String.class)
			.optional()
			.orElse(null);
	}

	private int memberCount() {
		return this.jdbc.sql("SELECT count(*) FROM membership WHERE project_id = ?")
			.param(UUID.fromString(this.project))
			.query(Integer.class)
			.single();
	}

	@SafeVarargs
	private static List<Integer> simultaneously(Callable<Integer>... requests) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(requests.length);
		try {
			CountDownLatch ready = new CountDownLatch(requests.length);
			CountDownLatch go = new CountDownLatch(1);
			List<Future<Integer>> results = new ArrayList<>();
			for (Callable<Integer> request : requests) {
				results.add(pool.submit(() -> {
					ready.countDown();
					go.await();
					return request.call();
				}));
			}
			ready.await();
			go.countDown();
			List<Integer> statuses = new ArrayList<>();
			for (Future<Integer> result : results) {
				statuses.add(result.get());
			}
			return statuses;
		}
		finally {
			pool.shutdownNow();
		}
	}

	private static int statusOf(ResultActions actions) {
		return actions.andReturn().getResponse().getStatus();
	}

	private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
		return request.header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(token));
	}

	private static JsonNode body(MvcResult result) throws Exception {
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString());
	}

}
