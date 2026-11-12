package com.matriz;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Task management through the API (openapi.yaml, {@code createTask}, {@code listTasks},
 * {@code getTask}, {@code updateTask}, {@code changeTaskState}, {@code deleteTask}; US-003, US-004,
 * US-005, US-011, US-017, US-018, US-024; EX-002, EX-011, EX-026; ADR-004, ADR-006), with real
 * persistence. Ana is the Project Owner, Rui a Project Participant, Eva a person who is not a member
 * of the project, and Daniel a member of no project. Each test adds its own people and project.
 */
@AutoConfigureMockMvc
class TaskApiTests extends DatabaseTestSupport {

	private static final MediaType MERGE_PATCH = MediaType.parseMediaType("application/merge-patch+json");

	private static final String INVALID = "The task is invalid.";

	private static final String PROJECT_NOT_FOUND = "The project does not exist or is not accessible.";

	private static final String TASK_NOT_FOUND = "The project or task does not exist or is not accessible.";

	private static final String NOT_OWNER = "Only the Project Owner of this project can do this.";

	private static final String NOT_OWNER_OR_ASSIGNEE = "Only the Project Owner or an assignee of the task can change its state.";

	private static final String COMPLETED = "The project is Completed; it cannot be changed.";

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

	@BeforeEach
	void addPeopleAndAProject() throws Exception {
		this.ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		this.rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		this.eva = TestAccess.addPerson(this.jdbc, "Eva Costa");
		this.daniel = TestAccess.addPerson(this.jdbc, "Daniel Reis");
		this.anaToken = TestAccess.accessToken(this.mvc, this.ana);
		this.ruiToken = TestAccess.accessToken(this.mvc, this.rui);
		this.evaToken = TestAccess.accessToken(this.mvc, this.eva);
		this.danielToken = TestAccess.accessToken(this.mvc, this.daniel);
		MvcResult result = this.mvc
			.perform(withToken(post("/api/projects"), this.anaToken).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Website Redesign\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		this.project = JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("projectId").stringValue();
		this.jdbc.sql("INSERT INTO membership (project_id, person_id, role) VALUES (?, ?, 'PROJECT_PARTICIPANT')")
			.params(UUID.fromString(this.project), this.rui.id())
			.update();
	}

	// Create ---------------------------------------------------------------------------------

	@Test
	void creatingATaskStartsItToDoWithPriorityMediumAndNoAssignee() throws Exception {
		MvcResult result = create(this.anaToken, "{\"title\":\"  Prepare proposal  \"}")
			.andExpect(status().isCreated())
			.andExpect(header().exists(HttpHeaders.LOCATION))
			.andReturn();

		String body = result.getResponse().getContentAsString();
		Contract.assertSatisfies("Task", body);
		JsonNode task = JsonMapper.shared().readTree(body);
		String id = task.path("taskId").stringValue();
		assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
			.isEqualTo("/api/projects/" + this.project + "/tasks/" + id);
		assertThat(task.path("projectId").stringValue()).isEqualTo(this.project);
		assertThat(task.path("title").stringValue()).isEqualTo("Prepare proposal");
		assertThat(task.path("state").stringValue()).isEqualTo("TO_DO");
		assertThat(task.path("priority").stringValue()).isEqualTo("MEDIUM");
		assertThat(task.has("description")).isFalse();
		assertThat(task.path("assignees")).isEmpty();
		assertThat(this.jdbc.sql("SELECT state, priority, description, deleted_at FROM task WHERE task_id = ?")
			.param(UUID.fromString(id))
			.query()
			.singleRow()).containsEntry("state", "TO_DO")
			.containsEntry("priority", "MEDIUM")
			.containsEntry("description", null)
			.containsEntry("deleted_at", null);
	}

	@Test
	void aTaskCanBeCreatedWithADescriptionAndAPriority() throws Exception {
		MvcResult result = create(this.anaToken,
				"{\"title\":\"Prepare budget\",\"description\":\" Numbers for Q1 \",\"priority\":\"HIGH\"}")
			.andExpect(status().isCreated())
			.andReturn();

		JsonNode task = JsonMapper.shared().readTree(result.getResponse().getContentAsString());
		Contract.assertSatisfies("Task", result.getResponse().getContentAsString());
		assertThat(task.path("description").stringValue()).isEqualTo("Numbers for Q1");
		assertThat(task.path("priority").stringValue()).isEqualTo("HIGH");
		assertThat(task.path("state").stringValue()).isEqualTo("TO_DO");
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"title\":\"T\",\"description\":null}", "{\"title\":\"T\",\"description\":\"   \"}",
			"{\"title\":\"T\",\"description\":\"\"}", "{\"title\":\"T\",\"description\":\"\\u00A0\"}" })
	void aDescriptionThatIsNullOrEmptyAfterTrimmingIsStoredAsNone(String content) throws Exception {
		String id = idOf(create(this.anaToken, content).andExpect(status().isCreated()).andReturn());

		assertThat(count("SELECT count(*) FROM task WHERE task_id = ? AND description IS NULL", UUID.fromString(id)))
			.isEqualTo(1);
		assertThat(view(this.anaToken, id).has("description")).isFalse();
	}

	@Test
	void theLimitsOfTheTitleAndTheDescriptionAreInclusive() throws Exception {
		create(this.anaToken, json("t", "d")).andExpect(status().isCreated());
		create(this.anaToken, json("t".repeat(150), "d".repeat(2000))).andExpect(status().isCreated());
		// Characters, not UTF-16 units: 150 emoji are 300 units.
		create(this.anaToken, json("\uD83D\uDE00".repeat(150), "\uD83D\uDE00".repeat(2000)))
			.andExpect(status().isCreated());
		assertThat(count("SELECT count(*) FROM task WHERE project_id = ?", UUID.fromString(this.project))).isEqualTo(3);

		assertInvalid(create(this.anaToken, json("t".repeat(151), null)), "/title",
				"The title must have at most 150 characters.");
		assertInvalid(create(this.anaToken, json("t", "d".repeat(2001))), "/description",
				"The description must have at most 2000 characters.");
		assertInvalid(create(this.anaToken, json("\uD83D\uDE00".repeat(151), null)), "/title",
				"The title must have at most 150 characters.");
		assertThat(count("SELECT count(*) FROM task WHERE project_id = ?", UUID.fromString(this.project))).isEqualTo(3);
	}

	@Test
	void theLimitsApplyAfterTrimming() throws Exception {
		create(this.anaToken, json("  " + "t".repeat(150) + "\u00A0 ", "\n" + "d".repeat(2000) + " "))
			.andExpect(status().isCreated());
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "     ", "\t\n", "\u00A0", "\u2007\u202F", " \u00A0\u202F\u2007 ",
			"\u3000", "\u2003" })
	void aTitleThatIsEmptyAfterTrimmingIsRefused(String title) throws Exception {
		assertInvalid(create(this.anaToken, json(title, null)), "/title", "A title is required.");

		assertThat(count("SELECT count(*) FROM task WHERE project_id = ?", UUID.fromString(this.project))).isZero();
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"title\":\"T\",\"priority\":\"High\"}", "{\"title\":\"T\",\"priority\":\"URGENT\"}",
			"{\"title\":\"T\",\"priority\":\"\"}", "{\"title\":\"T\",\"priority\":3}",
			"{\"title\":\"T\",\"priority\":null}" })
	void aPriorityThatIsNotACodeIsRefused(String content) throws Exception {
		assertInvalid(create(this.anaToken, content), "/priority", "Use one of LOW, MEDIUM, or HIGH.");

		assertThat(count("SELECT count(*) FROM task WHERE project_id = ?", UUID.fromString(this.project))).isZero();
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"title\":null}", "{\"title\":7}", "{\"title\":[\"a\"]}",
			"{\"title\":\"T\",\"description\":5}", "{\"title\":\"T\",\"state\":\"DONE\"}",
			"{\"title\":\"T\",\"assignees\":[]}", "{\"title\":\"T\",\"taskId\":\"x\"}", "[]", "null", "\"T\"", "7" })
	void contentThatIsNotAValidObjectIsRefusedWith422(String content) throws Exception {
		assertProblem(create(this.anaToken, content), 422, "Unprocessable Content", INVALID);

		assertThat(count("SELECT count(*) FROM task WHERE project_id = ?", UUID.fromString(this.project))).isZero();
	}

	@Test
	void everyInvalidMemberIsReportedAtOnce() throws Exception {
		MvcResult result = assertProblem(create(this.anaToken,
				"{\"title\":\" \",\"description\":\"" + "d".repeat(2001) + "\",\"priority\":\"x\"}"), 422,
				"Unprocessable Content", INVALID);

		JsonNode errors = JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("errors");
		assertThat(errors.valueStream().map((error) -> error.path("pointer").stringValue()).toList())
			.containsExactlyInAnyOrder("/title", "/description", "/priority");
		Contract.assertSatisfies("ValidationProblem", result.getResponse().getContentAsString());
	}

	@Test
	void contentThatIsNotWellFormedJsonIsRefusedWith400() throws Exception {
		assertProblem(createRaw(this.anaToken, "{\"title\":"), 400, "Bad Request",
				"The request content is not well-formed JSON.");
		assertProblem(createRaw(this.anaToken, ""), 400, "Bad Request", "The request content is not well-formed JSON.");
	}

	@Test
	void aMediaTypeThatIsNotJsonIsRefusedWith415() throws Exception {
		this.mvc.perform(withToken(post(tasks()), this.anaToken).contentType(MediaType.TEXT_PLAIN).content("{\"title\":\"T\"}"))
			.andExpect(status().isUnsupportedMediaType());
		this.mvc.perform(withToken(post(tasks()), this.anaToken).content("{\"title\":\"T\"}"))
			.andExpect(status().isUnsupportedMediaType());
		this.mvc.perform(withToken(post(tasks()), this.anaToken).contentType(MERGE_PATCH).content("{\"title\":\"T\"}"))
			.andExpect(status().isUnsupportedMediaType());

		assertThat(count("SELECT count(*) FROM task WHERE project_id = ?", UUID.fromString(this.project))).isZero();
	}

	// List, view, filters, and pages -----------------------------------------------------------

	@Test
	void tasksAreListedOldestFirstWithTheirDetails() throws Exception {
		String first = createdId("First", null, null);
		String second = createdId("Second", "Described", "HIGH");
		String third = createdId("Third", null, "LOW");

		JsonNode page = list(this.ruiToken, "");

		assertThat(titles(page)).containsExactly("First", "Second", "Third");
		assertThat(page.path("page").intValue()).isEqualTo(1);
		assertThat(page.path("size").intValue()).isEqualTo(20);
		assertThat(page.path("totalItems").intValue()).isEqualTo(3);
		assertThat(page.path("totalPages").intValue()).isEqualTo(1);
		assertThat(page.path("items").get(1).path("description").stringValue()).isEqualTo("Described");
		assertThat(view(this.ruiToken, first).path("title").stringValue()).isEqualTo("First");
		assertThat(view(this.ruiToken, second).path("priority").stringValue()).isEqualTo("HIGH");
		assertThat(view(this.ruiToken, third).path("priority").stringValue()).isEqualTo("LOW");
	}

	@Test
	void anEmptyProjectHasAnEmptyPage() throws Exception {
		JsonNode page = list(this.anaToken, "");

		assertThat(page.path("items")).isEmpty();
		assertThat(page.path("totalItems").intValue()).isZero();
		assertThat(page.path("totalPages").intValue()).isZero();
	}

	@Test
	void tasksAreCutIntoPagesInCreationOrder() throws Exception {
		for (int i = 1; i <= 5; i++) {
			createdId("Task " + i, null, null);
		}

		JsonNode first = list(this.anaToken, "?size=2");
		JsonNode second = list(this.anaToken, "?size=2&page=2");
		JsonNode last = list(this.anaToken, "?size=2&page=3");
		JsonNode beyond = list(this.anaToken, "?size=2&page=4");

		assertThat(titles(first)).containsExactly("Task 1", "Task 2");
		assertThat(titles(second)).containsExactly("Task 3", "Task 4");
		assertThat(titles(last)).containsExactly("Task 5");
		assertThat(titles(beyond)).isEmpty();
		assertThat(beyond.path("page").intValue()).isEqualTo(4);
		assertThat(beyond.path("totalItems").intValue()).isEqualTo(5);
		assertThat(beyond.path("totalPages").intValue()).isEqualTo(3);
		assertThat(list(this.anaToken, "?size=100").path("size").intValue()).isEqualTo(100);
		assertThat(list(this.anaToken, "?size=1").path("totalPages").intValue()).isEqualTo(5);
	}

	@Test
	void aPageOrSizeOutsideItsRangeIsRefusedWith400() throws Exception {
		createdId("One", null, null);
		String page = "The query parameter page must be an integer of at least 1.";
		String size = "The query parameter size must be an integer from 1 to 100.";

		for (String query : List.of("?page=0", "?page=-1", "?page=x", "?page=", "?page=1.5", "?page=99999999999")) {
			assertProblem(getAs(this.anaToken, tasks() + query), 400, "Bad Request", page);
		}
		for (String query : List.of("?size=0", "?size=101", "?size=x", "?size=")) {
			assertProblem(getAs(this.anaToken, tasks() + query), 400, "Bad Request", size);
		}
	}

	@Test
	void theLargestAcceptedTaskPageIsAnEmptyPageNotAServerError() throws Exception {
		createdId("One", null, null);
		JsonNode page = list(this.anaToken, "?page=2147483647&size=100");
		assertThat(page.path("items")).isEmpty();
		assertThat(page.path("totalItems").intValue()).isEqualTo(1);
	}

	@Test
	void tasksCanBeFilteredByStateByPriorityAndByBoth() throws Exception {
		createdId("A", null, "HIGH");
		String b = createdId("B", null, "HIGH");
		createdId("C", null, "LOW");
		String d = createdId("D", null, "MEDIUM");
		changeState(this.anaToken, b, "IN_PROGRESS").andExpect(status().isOk());
		changeState(this.anaToken, d, "DONE").andExpect(status().isOk());

		assertThat(titles(list(this.anaToken, "?state=TO_DO"))).containsExactly("A", "C");
		assertThat(titles(list(this.anaToken, "?state=TO_DO&state=IN_PROGRESS"))).containsExactly("A", "B", "C");
		assertThat(titles(list(this.anaToken, "?priority=HIGH"))).containsExactly("A", "B");
		assertThat(titles(list(this.anaToken, "?priority=HIGH&priority=LOW"))).containsExactly("A", "B", "C");
		assertThat(titles(list(this.anaToken, "?state=TO_DO&priority=HIGH"))).containsExactly("A");
		assertThat(titles(list(this.anaToken, "?state=TO_DO&state=DONE&priority=HIGH&priority=MEDIUM")))
			.containsExactly("A", "D");
		assertThat(titles(list(this.anaToken, "?state=TO_DO&state=TO_DO"))).containsExactly("A", "C");
	}

	@Test
	void aFilterWithNoMatchGivesAnEmptyPageAndFiltersCombineWithPages() throws Exception {
		for (int i = 1; i <= 3; i++) {
			createdId("High " + i, null, "HIGH");
		}
		createdId("Low", null, "LOW");

		JsonNode none = list(this.anaToken, "?state=DONE");
		JsonNode paged = list(this.anaToken, "?priority=HIGH&size=2&page=2");

		assertThat(none.path("items")).isEmpty();
		assertThat(none.path("totalItems").intValue()).isZero();
		assertThat(none.path("totalPages").intValue()).isZero();
		assertThat(titles(paged)).containsExactly("High 3");
		assertThat(paged.path("totalItems").intValue()).isEqualTo(3);
		assertThat(paged.path("totalPages").intValue()).isEqualTo(2);
	}

	@Test
	void aFilterValueThatIsNotACodeIsRefusedWith400() throws Exception {
		createdId("One", null, null);
		String state = "The query parameter state must be TO_DO, IN_PROGRESS, or DONE.";
		String priority = "The query parameter priority must be LOW, MEDIUM, or HIGH.";

		for (String query : List.of("?state=Done", "?state=", "?state=TO_DO&state=BLOCKED", "?state=TO_DO,DONE",
				"?priority=LOW&state=nope")) {
			assertProblem(getAs(this.anaToken, tasks() + query), 400, "Bad Request", state);
		}
		for (String query : List.of("?priority=High", "?priority=", "?priority=URGENT&state=DONE")) {
			assertProblem(getAs(this.anaToken, tasks() + query), 400, "Bad Request", priority);
		}
	}

	// Edit, state, and delete ------------------------------------------------------------------

	@Test
	void theTitleTheDescriptionAndThePriorityAreChangedTogetherOrApart() throws Exception {
		String id = createdId("Old", "Before", "MEDIUM");

		JsonNode renamed = body(edit(this.anaToken, id, "{\"title\":\"  New title \"}"));
		assertThat(renamed.path("title").stringValue()).isEqualTo("New title");
		assertThat(renamed.path("description").stringValue()).isEqualTo("Before");
		assertThat(renamed.path("priority").stringValue()).isEqualTo("MEDIUM");

		JsonNode described = body(edit(this.anaToken, id, "{\"description\":\" After \"}"));
		assertThat(described.path("title").stringValue()).isEqualTo("New title");
		assertThat(described.path("description").stringValue()).isEqualTo("After");

		JsonNode prioritised = body(edit(this.anaToken, id, "{\"priority\":\"LOW\"}"));
		assertThat(prioritised.path("priority").stringValue()).isEqualTo("LOW");
		assertThat(prioritised.path("description").stringValue()).isEqualTo("After");

		JsonNode all = body(edit(this.anaToken, id, "{\"title\":\"All\",\"description\":\"Three\",\"priority\":\"HIGH\"}"));
		assertThat(all.path("title").stringValue()).isEqualTo("All");
		assertThat(all.path("description").stringValue()).isEqualTo("Three");
		assertThat(all.path("priority").stringValue()).isEqualTo("HIGH");
		assertThat(view(this.ruiToken, id)).isEqualTo(all);
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"description\":null}", "{\"description\":\"\"}", "{\"description\":\"  \\u00A0\"}" })
	void aDescriptionThatIsNullOrEmptyRemovesIt(String content) throws Exception {
		String id = createdId("Task", "Something", null);

		JsonNode task = body(edit(this.anaToken, id, content));

		assertThat(task.has("description")).isFalse();
		assertThat(count("SELECT count(*) FROM task WHERE task_id = ? AND description IS NULL", UUID.fromString(id)))
			.isEqualTo(1);
	}

	@Test
	void settingTheCurrentValuesSucceedsAndChangesNothing() throws Exception {
		String id = createdId("Same", "Same", "HIGH");

		JsonNode task = body(edit(this.anaToken, id, "{\"title\":\"Same\",\"description\":\"Same\",\"priority\":\"HIGH\"}"));

		assertThat(task).isEqualTo(view(this.anaToken, id));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"title\":\"\"}", "{\"title\":\" \"}", "{\"title\":\"\\u00A0\"}",
			"{\"title\":\"\\u2007\\u202F\"}" })
	void aTitleThatIsEmptyAfterTrimmingIsRefusedAndNothingChanges(String content) throws Exception {
		String id = createdId("Keep", "Keep", "HIGH");
		JsonNode before = view(this.anaToken, id);

		assertInvalid(edit(this.anaToken, id, content), "/title", "A title is required.");

		assertThat(view(this.anaToken, id)).isEqualTo(before);
	}

	@Test
	void theLimitsAreInclusiveWhenEditingToo() throws Exception {
		String id = createdId("Keep", null, null);

		edit(this.anaToken, id, json("t".repeat(150), "d".repeat(2000))).andExpect(status().isOk());
		JsonNode before = view(this.anaToken, id);
		assertInvalid(edit(this.anaToken, id, "{\"title\":\"" + "t".repeat(151) + "\"}"), "/title",
				"The title must have at most 150 characters.");
		assertInvalid(edit(this.anaToken, id, "{\"description\":\"" + "d".repeat(2001) + "\"}"), "/description",
				"The description must have at most 2000 characters.");

		assertThat(view(this.anaToken, id)).isEqualTo(before);
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"title\":null}", "{\"priority\":null}", "{\"priority\":\"Low\"}",
			"{\"title\":7}", "{\"description\":5}", "{\"state\":\"DONE\"}", "{\"assignees\":[]}", "[]", "null", "\"x\"",
			"{\"title\":\"Changed\",\"priority\":\"urgent\"}" })
	void invalidContentIsRefusedWith422AndNothingChanges(String content) throws Exception {
		String id = createdId("Keep", "Keep", "HIGH");
		JsonNode before = view(this.anaToken, id);

		assertProblem(edit(this.anaToken, id, content), 422, "Unprocessable Content", INVALID);

		assertThat(view(this.anaToken, id)).isEqualTo(before);
	}

	@Test
	void anEmptyPatchNamesTheContentAsTheInvalidMember() throws Exception {
		String id = createdId("Keep", null, null);

		assertInvalid(edit(this.anaToken, id, "{}"), "",
				"At least one of title, description, and priority is required.");
	}

	@Test
	void aPatchNeedsTheMergePatchMediaTypeAndWellFormedJson() throws Exception {
		String id = createdId("Keep", null, null);

		this.mvc
			.perform(withToken(patch(tasks() + "/" + id), this.anaToken).contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"X\"}"))
			.andExpect(status().isUnsupportedMediaType());
		assertProblem(this.mvc.perform(withToken(patch(tasks() + "/" + id), this.anaToken).contentType(MERGE_PATCH)
			.content("{\"title\":")), 400, "Bad Request", "The request content is not well-formed JSON.");

		assertThat(view(this.anaToken, id).path("title").stringValue()).isEqualTo("Keep");
	}

	@Test
	void anyStateCanBeSetFromAnyState() throws Exception {
		String id = createdId("Moving", null, null);

		for (String state : List.of("IN_PROGRESS", "DONE", "TO_DO", "DONE", "IN_PROGRESS", "TO_DO", "TO_DO")) {
			JsonNode task = body(changeState(this.anaToken, id, state));
			assertThat(task.path("state").stringValue()).isEqualTo(state);
			assertThat(view(this.ruiToken, id).path("state").stringValue()).isEqualTo(state);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"state\":\"Done\"}", "{\"state\":\"\"}", "{\"state\":null}", "{\"state\":2}",
			"{}", "{\"state\":\"DONE\",\"title\":\"x\"}", "[]", "null", "\"DONE\"" })
	void aStateThatIsNotACodeIsRefusedAndTheStateIsUnchanged(String content) throws Exception {
		String id = createdId("Stay", null, null);

		assertProblem(this.mvc.perform(withToken(put(tasks() + "/" + id + "/state"), this.anaToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(content)), 422, "Unprocessable Content", INVALID);

		assertThat(view(this.anaToken, id).path("state").stringValue()).isEqualTo("TO_DO");
	}

	@Test
	void theStateOfATaskNeedsPutAndJson() throws Exception {
		String id = createdId("Stay", null, null);

		this.mvc.perform(withToken(post(tasks() + "/" + id + "/state"), this.anaToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"state\":\"DONE\"}")).andExpect(status().isMethodNotAllowed());
		this.mvc.perform(withToken(put(tasks() + "/" + id + "/state"), this.anaToken)
			.contentType(MediaType.TEXT_PLAIN)
			.content("{\"state\":\"DONE\"}")).andExpect(status().isUnsupportedMediaType());
		assertProblem(this.mvc.perform(withToken(put(tasks() + "/" + id + "/state"), this.anaToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"state\":")), 400, "Bad Request", "The request content is not well-formed JSON.");

		assertThat(view(this.anaToken, id).path("state").stringValue()).isEqualTo("TO_DO");
	}

	@Test
	void aDeletedTaskIsInNoReadPathAndItsAssignmentsAreRemoved() throws Exception {
		String doomed = createdId("Doomed", null, null);
		String kept = createdId("Kept", null, null);
		assign(doomed, this.rui);
		assign(doomed, this.ana);
		assign(kept, this.rui);
		UUID comment = UUID.randomUUID();
		this.jdbc.sql("INSERT INTO comment (comment_id, task_id, author_person_id, text, created_at) "
				+ "VALUES (?, ?, ?, 'A comment', now())")
			.params(comment, UUID.fromString(doomed), this.rui.id())
			.update();
		assertThat(view(this.anaToken, doomed).path("assignees")).hasSize(2);

		deleteRequest(this.anaToken, doomed).andExpect(status().isNoContent());

		assertProblem(getAs(this.anaToken, tasks() + "/" + doomed), 404, "Not Found", TASK_NOT_FOUND);
		assertProblem(getAs(this.ruiToken, tasks() + "/" + doomed), 404, "Not Found", TASK_NOT_FOUND);
		assertThat(titles(list(this.anaToken, ""))).containsExactly("Kept");
		assertThat(titles(list(this.ruiToken, "?state=TO_DO"))).containsExactly("Kept");
		assertThat(list(this.anaToken, "").path("totalItems").intValue()).isEqualTo(1);
		assertProblem(deleteRequest(this.anaToken, doomed), 404, "Not Found", TASK_NOT_FOUND);
		assertProblem(edit(this.anaToken, doomed, "{\"title\":\"Back\"}"), 404, "Not Found", TASK_NOT_FOUND);
		assertProblem(changeState(this.anaToken, doomed, "DONE"), 404, "Not Found", TASK_NOT_FOUND);
		assertThat(count("SELECT count(*) FROM task WHERE task_id = ? AND deleted_at IS NOT NULL",
				UUID.fromString(doomed))).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM assignment WHERE task_id = ?", UUID.fromString(doomed))).isZero();
		assertThat(count("SELECT count(*) FROM assignment WHERE task_id = ?", UUID.fromString(kept))).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM comment WHERE comment_id = ?", comment)).isEqualTo(1);
		assertNoOrphans();
		assertThat(view(this.anaToken, kept).path("assignees")).hasSize(1);
	}

	@Test
	void theAssigneesOfATaskAreShownByDisplayName() throws Exception {
		String id = createdId("Shared", null, null);
		assign(id, this.rui);
		assign(id, this.ana);

		JsonNode task = view(this.anaToken, id);

		assertThat(task.path("assignees").valueStream().map((a) -> a.path("displayName").stringValue()).toList())
			.containsExactly("Ana Silva", "Rui Martins");
		assertThat(task.path("assignees").get(0).path("personId").stringValue()).isEqualTo(this.ana.id().toString());
	}

	// Access ---------------------------------------------------------------------------------

	@Test
	void aPersonWhoIsNotAMemberGets404ForEveryOperationAndNothingChanges() throws Exception {
		String id = createdId("Private", "Secret", "HIGH");
		JsonNode before = view(this.anaToken, id);

		for (String token : List.of(this.evaToken, this.danielToken)) {
			assertProblem(create(token, json("Intruder", null)), 404, "Not Found", PROJECT_NOT_FOUND);
			assertProblem(getAs(token, tasks()), 404, "Not Found", PROJECT_NOT_FOUND);
			assertProblem(getAs(token, tasks() + "?state=DONE"), 404, "Not Found", PROJECT_NOT_FOUND);
			assertProblem(getAs(token, tasks() + "/" + id), 404, "Not Found", TASK_NOT_FOUND);
			assertProblem(edit(token, id, "{\"title\":\"Hacked\"}"), 404, "Not Found", TASK_NOT_FOUND);
			assertProblem(changeState(token, id, "DONE"), 404, "Not Found", TASK_NOT_FOUND);
			assertProblem(deleteRequest(token, id), 404, "Not Found", TASK_NOT_FOUND);
			// The answer does not depend on the content, so nothing is learned from the order of checks.
			assertProblem(edit(token, id, "{}"), 404, "Not Found", TASK_NOT_FOUND);
			assertProblem(createRaw(token, "{"), 404, "Not Found", PROJECT_NOT_FOUND);
		}

		assertThat(view(this.anaToken, id)).isEqualTo(before);
		assertThat(count("SELECT count(*) FROM task WHERE project_id = ?", UUID.fromString(this.project))).isEqualTo(1);
	}

	@Test
	void aProjectOrTaskThatDoesNotExistOrIsNotACanonicalIdentifierGets404() throws Exception {
		String id = createdId("Real", null, null);
		String other = UUID.randomUUID().toString();

		assertProblem(getAs(this.anaToken, "/api/projects/" + other + "/tasks"), 404, "Not Found", PROJECT_NOT_FOUND);
		assertProblem(getAs(this.anaToken, "/api/projects/not-an-id/tasks"), 404, "Not Found", PROJECT_NOT_FOUND);
		assertProblem(getAs(this.anaToken, tasks() + "/" + other), 404, "Not Found", TASK_NOT_FOUND);
		assertProblem(getAs(this.anaToken, tasks() + "/not-an-id"), 404, "Not Found", TASK_NOT_FOUND);
		assertProblem(getAs(this.anaToken, tasks() + "/" + id.toUpperCase().replace("-", "")), 404, "Not Found",
				TASK_NOT_FOUND);
		assertProblem(changeState(this.anaToken, other, "DONE"), 404, "Not Found", TASK_NOT_FOUND);
	}

	@Test
	void aTaskOfAnotherProjectIsNotFoundThroughThisOne() throws Exception {
		createdId("Mine", null, null);
		String otherProject = createProjectAs(this.evaToken, "Eva's");
		String theirs = idOf(this.mvc
			.perform(withToken(post("/api/projects/" + otherProject + "/tasks"), this.evaToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("Theirs", null)))
			.andExpect(status().isCreated())
			.andReturn());

		assertProblem(getAs(this.anaToken, tasks() + "/" + theirs), 404, "Not Found", TASK_NOT_FOUND);
		assertProblem(edit(this.anaToken, theirs, "{\"title\":\"Hacked\"}"), 404, "Not Found", TASK_NOT_FOUND);
		assertProblem(changeState(this.anaToken, theirs, "DONE"), 404, "Not Found", TASK_NOT_FOUND);
		assertProblem(deleteRequest(this.anaToken, theirs), 404, "Not Found", TASK_NOT_FOUND);

		assertThat(titles(list(this.anaToken, ""))).containsExactly("Mine");
		assertThat(count("SELECT count(*) FROM task WHERE task_id = ? AND title = 'Theirs' AND deleted_at IS NULL",
				UUID.fromString(theirs))).isEqualTo(1);
	}

	@Test
	void withoutATokenEveryOperationGets401() throws Exception {
		String id = createdId("Guarded", null, null);
		String base = tasks();

		TestAccess.assertNoToken(this.mvc.perform(post(base).contentType(MediaType.APPLICATION_JSON).content(json("x", null))));
		TestAccess.assertNoToken(this.mvc.perform(get(base)));
		TestAccess.assertNoToken(this.mvc.perform(get(base + "/" + id)));
		TestAccess.assertNoToken(this.mvc.perform(patch(base + "/" + id).contentType(MERGE_PATCH).content("{\"title\":\"x\"}")));
		TestAccess.assertNoToken(this.mvc.perform(put(base + "/" + id + "/state").contentType(MediaType.APPLICATION_JSON)
			.content("{\"state\":\"DONE\"}")));
		TestAccess.assertNoToken(this.mvc.perform(delete(base + "/" + id)));

		assertThat(view(this.anaToken, id).path("title").stringValue()).isEqualTo("Guarded");
		assertThat(count("SELECT count(*) FROM task WHERE project_id = ?", UUID.fromString(this.project))).isEqualTo(1);
	}

	@Test
	void anEndedTokenGets401() throws Exception {
		String token = TestAccess.accessToken(this.mvc, this.ana);
		TestAccess.signOut(this.mvc, token);

		TestAccess.assertInvalidToken(this.mvc.perform(withToken(get(tasks()), token)));
	}

	@Test
	void aProjectParticipantMayViewAndListButNotChangeAnything() throws Exception {
		String id = createdId("Look only", "Text", "HIGH");
		JsonNode before = view(this.anaToken, id);

		assertThat(view(this.ruiToken, id)).isEqualTo(before);
		assertThat(titles(list(this.ruiToken, ""))).containsExactly("Look only");
		assertProblem(create(this.ruiToken, json("Mine", null)), 403, "Forbidden", NOT_OWNER);
		assertProblem(edit(this.ruiToken, id, "{\"title\":\"Mine\"}"), 403, "Forbidden", NOT_OWNER);
		assertProblem(changeState(this.ruiToken, id, "DONE"), 403, "Forbidden", NOT_OWNER_OR_ASSIGNEE);
		assertProblem(deleteRequest(this.ruiToken, id), 403, "Forbidden", NOT_OWNER);

		assertThat(view(this.anaToken, id)).isEqualTo(before);
		assertThat(count("SELECT count(*) FROM task WHERE project_id = ?", UUID.fromString(this.project))).isEqualTo(1);
	}

	@Test
	void theChecksComeInTheOrderOfTheDecision() throws Exception {
		String id = createdId("Order", null, null);

		// 403 before 415, 400, and 422
		this.mvc.perform(withToken(post(tasks()), this.ruiToken).contentType(MediaType.TEXT_PLAIN).content("x"))
			.andExpect(status().isForbidden());
		assertProblem(createRaw(this.ruiToken, "{"), 403, "Forbidden", NOT_OWNER);
		assertProblem(edit(this.ruiToken, id, "{}"), 403, "Forbidden", NOT_OWNER);
		assertProblem(changeState(this.ruiToken, id, "nope"), 403, "Forbidden", NOT_OWNER_OR_ASSIGNEE);
		// 415 and 400 before 422
		this.mvc.perform(withToken(patch(tasks() + "/" + id), this.anaToken).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isUnsupportedMediaType());
		// 422 before 409
		complete();
		assertProblem(create(this.anaToken, json(" ", null)), 422, "Unprocessable Content", INVALID);
		assertProblem(edit(this.anaToken, id, "{}"), 422, "Unprocessable Content", INVALID);
		assertProblem(changeState(this.anaToken, id, "nope"), 422, "Unprocessable Content", INVALID);
		// 404 and 403 before 409
		assertProblem(createRaw(this.evaToken, "{"), 404, "Not Found", PROJECT_NOT_FOUND);
		assertProblem(deleteRequest(this.ruiToken, id), 403, "Forbidden", NOT_OWNER);
		assertProblem(deleteRequest(this.anaToken, id), 409, "Conflict", COMPLETED);
	}

	// Completed project ------------------------------------------------------------------------

	@Test
	void aCompletedProjectRefusesEveryChangeButStillShowsItsTasks() throws Exception {
		String id = createdId("Frozen", "Text", "HIGH");
		assign(id, this.rui);
		complete();
		JsonNode before = view(this.anaToken, id);

		assertProblem(create(this.anaToken, json("New", null)), 409, "Conflict", COMPLETED);
		assertProblem(edit(this.anaToken, id, "{\"title\":\"Changed\"}"), 409, "Conflict", COMPLETED);
		assertProblem(changeState(this.anaToken, id, "DONE"), 409, "Conflict", COMPLETED);
		assertProblem(deleteRequest(this.anaToken, id), 409, "Conflict", COMPLETED);

		assertThat(view(this.anaToken, id)).isEqualTo(before);
		assertThat(view(this.ruiToken, id)).isEqualTo(before);
		assertThat(titles(list(this.anaToken, ""))).containsExactly("Frozen");
		assertThat(titles(list(this.ruiToken, "?state=TO_DO&priority=HIGH"))).containsExactly("Frozen");
		assertThat(count("SELECT count(*) FROM task WHERE project_id = ?", UUID.fromString(this.project))).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM assignment WHERE task_id = ?", UUID.fromString(id))).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM task WHERE project_id = ? AND deleted_at IS NOT NULL",
				UUID.fromString(this.project)))
			.isZero();
	}

	@Test
	void aTaskCanBeChangedAgainWhenTheProjectIsActiveAgain() throws Exception {
		String id = createdId("Thawed", null, null);
		complete();
		assertProblem(changeState(this.anaToken, id, "DONE"), 409, "Conflict", COMPLETED);
		this.jdbc.sql("UPDATE project SET state = 'ACTIVE' WHERE project_id = ?").param(UUID.fromString(this.project)).update();

		assertThat(body(changeState(this.anaToken, id, "DONE")).path("state").stringValue()).isEqualTo("DONE");
	}

	// Simultaneous changes and persistence -------------------------------------------------------

	@Test
	void twoSimultaneousChangesAreAppliedOneAfterTheOtherAndTheLastPrevails() throws Exception {
		String id = createdId("Contended", null, null);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			CountDownLatch ready = new CountDownLatch(2);
			CountDownLatch go = new CountDownLatch(1);
			List<Callable<Integer>> changes = List.of(() -> stateAfterStarting(ready, go, id, "IN_PROGRESS"),
					() -> stateAfterStarting(ready, go, id, "DONE"));
			List<Future<Integer>> results = new ArrayList<>();
			for (Callable<Integer> change : changes) {
				results.add(pool.submit(change));
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

		assertThat(view(this.anaToken, id).path("state").stringValue()).isIn("IN_PROGRESS", "DONE");
	}

	@Test
	void aTaskStaysAfterSigningOutAndInAgain() throws Exception {
		String id = createdId("Lasting", "Described", "LOW");
		TestAccess.signOut(this.mvc, this.anaToken);

		String again = TestAccess.accessToken(this.mvc, this.ana);

		JsonNode task = view(again, id);
		assertThat(task.path("title").stringValue()).isEqualTo("Lasting");
		assertThat(titles(list(again, ""))).containsExactly("Lasting");
	}

	@Test
	void responsesSatisfyTheContract() throws Exception {
		String id = createdId("Contract", "Described", "HIGH");
		createdId("Contract two", null, null);

		Contract.assertSatisfies("TaskPage", getAs(this.anaToken, tasks()).andReturn().getResponse().getContentAsString());
		Contract.assertSatisfies("TaskPage",
				getAs(this.anaToken, tasks() + "?state=DONE").andReturn().getResponse().getContentAsString());
		Contract.assertSatisfies("Task", edit(this.anaToken, id, "{\"priority\":\"LOW\"}").andReturn().getResponse()
			.getContentAsString());
		Contract.assertSatisfies("Task", changeState(this.anaToken, id, "DONE").andReturn().getResponse()
			.getContentAsString());
	}

	// Helpers --------------------------------------------------------------------------------

	private Integer stateAfterStarting(CountDownLatch ready, CountDownLatch go, String id, String state)
			throws Exception {
		ready.countDown();
		go.await();
		return changeState(this.anaToken, id, state).andReturn().getResponse().getStatus();
	}

	private String tasks() {
		return "/api/projects/" + this.project + "/tasks";
	}

	private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
		return request.header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(token));
	}

	private ResultActions create(String token, String content) throws Exception {
		return this.mvc.perform(withToken(post(tasks()), token).contentType(MediaType.APPLICATION_JSON).content(content));
	}

	private ResultActions createRaw(String token, String content) throws Exception {
		return create(token, content);
	}

	private ResultActions edit(String token, String id, String content) throws Exception {
		return this.mvc.perform(withToken(patch(tasks() + "/" + id), token).contentType(MERGE_PATCH).content(content));
	}

	private ResultActions changeState(String token, String id, String state) throws Exception {
		return this.mvc.perform(withToken(put(tasks() + "/" + id + "/state"), token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"state\":\"" + state + "\"}"));
	}

	private ResultActions getAs(String token, String url) throws Exception {
		return this.mvc.perform(withToken(get(URI.create(url)), token));
	}

	private ResultActions deleteRequest(String token, String id) throws Exception {
		return this.mvc.perform(withToken(delete(tasks() + "/" + id), token));
	}

	private static String json(String title, String description) {
		return JsonMapper.shared().writeValueAsString(description == null ? Map.of("title", title)
				: Map.of("title", title, "description", description));
	}

	private String createdId(String title, String description, String priority) throws Exception {
		Map<String, String> content = new java.util.LinkedHashMap<>();
		content.put("title", title);
		if (description != null) {
			content.put("description", description);
		}
		if (priority != null) {
			content.put("priority", priority);
		}
		return idOf(create(this.anaToken, JsonMapper.shared().writeValueAsString(content))
			.andExpect(status().isCreated())
			.andReturn());
	}

	private String createProjectAs(String token, String name) throws Exception {
		MvcResult result = this.mvc
			.perform(withToken(post("/api/projects"), token).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"" + name + "\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("projectId").stringValue();
	}

	private static String idOf(MvcResult result) throws Exception {
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("taskId").stringValue();
	}

	private JsonNode body(ResultActions actions) throws Exception {
		MvcResult result = actions.andExpect(status().isOk()).andReturn();
		Contract.assertSatisfies("Task", result.getResponse().getContentAsString());
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString());
	}

	private JsonNode view(String token, String id) throws Exception {
		return body(getAs(token, tasks() + "/" + id));
	}

	private JsonNode list(String token, String query) throws Exception {
		MvcResult result = getAs(token, tasks() + query).andExpect(status().isOk()).andReturn();
		Contract.assertSatisfies("TaskPage", result.getResponse().getContentAsString());
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString());
	}

	private static List<String> titles(JsonNode page) {
		return page == null ? List.of()
				: page.path("items").valueStream().map((item) -> item.path("title").stringValue()).toList();
	}

	private void assertInvalid(ResultActions actions, String pointer, String detail) throws Exception {
		MvcResult result = assertProblem(actions, 422, "Unprocessable Content", INVALID);
		JsonNode errors = JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("errors");
		assertThat(errors.valueStream()
			.anyMatch((error) -> pointer.equals(error.path("pointer").stringValue())
					&& detail.equals(error.path("detail").stringValue()))).as("errors %s", errors).isTrue();
	}

	private void assign(String taskId, TestAccess.TestPerson person) {
		this.jdbc.sql("INSERT INTO assignment (task_id, person_id, project_id) VALUES (?, ?, ?)")
			.params(UUID.fromString(taskId), person.id(), UUID.fromString(this.project))
			.update();
	}

	private void complete() {
		this.jdbc.sql("UPDATE project SET state = 'COMPLETED' WHERE project_id = ?")
			.param(UUID.fromString(this.project))
			.update();
	}

	private void assertNoOrphans() {
		assertThat(count("SELECT count(*) FROM task t WHERE NOT EXISTS (SELECT 1 FROM project p WHERE p.project_id = t.project_id)")).isZero();
		assertThat(count("SELECT count(*) FROM assignment a WHERE NOT EXISTS (SELECT 1 FROM task t WHERE t.task_id = a.task_id)")).isZero();
		assertThat(count("SELECT count(*) FROM comment c WHERE NOT EXISTS (SELECT 1 FROM task t WHERE t.task_id = c.task_id)")).isZero();
	}

	private int count(String sql, Object... parameters) {
		return this.jdbc.sql(sql).params(parameters).query(Integer.class).single();
	}

}
