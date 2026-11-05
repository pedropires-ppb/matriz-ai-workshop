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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Project management through the API (openapi.yaml, {@code createProject}, {@code listProjects},
 * {@code getProject}, {@code updateProject}, {@code deleteProject}; US-001, US-007, US-008,
 * US-016, US-023; EX-001, EX-010; ADR-004, ADR-009), with real persistence. Each test adds its own
 * people, so lists are never shared between tests.
 */
@AutoConfigureMockMvc
class ProjectApiTests extends DatabaseTestSupport {

	private static final String PROJECTS = "/api/projects";

	private static final MediaType MERGE_PATCH = MediaType.parseMediaType("application/merge-patch+json");

	private static final String INVALID = "The project is invalid.";

	private static final String NOT_FOUND = "The project does not exist or is not accessible.";

	private static final String NOT_OWNER = "Only the Project Owner of this project can do this.";

	private static final String COMPLETED = "The project is Completed; it cannot be changed.";

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

	@BeforeEach
	void addPeople() throws Exception {
		this.ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		this.rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		this.eva = TestAccess.addPerson(this.jdbc, "Eva Costa");
		this.anaToken = TestAccess.accessToken(this.mvc, this.ana);
		this.ruiToken = TestAccess.accessToken(this.mvc, this.rui);
		this.evaToken = TestAccess.accessToken(this.mvc, this.eva);
	}

	// Create ---------------------------------------------------------------------------------

	@Test
	void creatingAProjectMakesTheSignedInPersonItsProjectOwner() throws Exception {
		MvcResult result = create(this.anaToken, "{\"name\":\"  Website Redesign  \",\"description\":\" New site \"}")
			.andExpect(status().isCreated())
			.andExpect(header().exists(HttpHeaders.LOCATION))
			.andReturn();

		String body = result.getResponse().getContentAsString();
		Contract.assertSatisfies("Project", body);
		JsonNode project = JsonMapper.shared().readTree(body);
		String id = project.path("projectId").stringValue();
		assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo(PROJECTS + "/" + id);
		assertThat(project.path("name").stringValue()).isEqualTo("Website Redesign");
		assertThat(project.path("description").stringValue()).isEqualTo("New site");
		assertThat(project.path("state").stringValue()).isEqualTo("ACTIVE");
		assertThat(project.path("myRole").stringValue()).isEqualTo("PROJECT_OWNER");
		assertThat(this.jdbc.sql("SELECT person_id, role FROM membership WHERE project_id = ?")
			.param(UUID.fromString(id))
			.query()
			.listOfRows()).hasSize(1)
			.first()
			.satisfies((row) -> {
				assertThat(row).containsEntry("person_id", this.ana.id()).containsEntry("role", "PROJECT_OWNER");
			});
	}

	@Test
	void aProjectWithoutADescriptionHasNone() throws Exception {
		String body = create(this.anaToken, "{\"name\":\"Plain\"}").andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		Contract.assertSatisfies("Project", body);
		assertThat(JsonMapper.shared().readTree(body).has("description")).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"name\":\"Plain\",\"description\":null}", "{\"name\":\"Plain\",\"description\":\"   \"}",
			"{\"name\":\"Plain\",\"description\":\"\"}" })
	void aDescriptionThatIsNullOrEmptyAfterTrimmingIsStoredAsNone(String content) throws Exception {
		String id = idOf(create(this.anaToken, content).andExpect(status().isCreated()).andReturn());

		assertThat(this.jdbc.sql("SELECT description FROM project WHERE project_id = ?")
			.param(UUID.fromString(id))
			.query(String.class)
			.optional()).isEmpty();
		assertThat(this.jdbc.sql("SELECT count(*) FROM project WHERE project_id = ? AND description IS NULL")
			.param(UUID.fromString(id))
			.query(Integer.class)
			.single()).isEqualTo(1);
	}

	@Test
	void theLimitsOfTheNameAndTheDescriptionAreInclusive() throws Exception {
		create(this.anaToken, json("a", "d")).andExpect(status().isCreated());
		create(this.anaToken, json("n".repeat(100), "d".repeat(1000))).andExpect(status().isCreated());
		// Characters, not UTF-16 units: 100 emoji are 200 units.
		create(this.anaToken, json("\uD83D\uDE00".repeat(100), null)).andExpect(status().isCreated());

		assertInvalid(create(this.anaToken, json("n".repeat(101), null)), "/name",
				"The name must have at most 100 characters.");
		assertInvalid(create(this.anaToken, json("\uD83D\uDE00".repeat(101), null)), "/name",
				"The name must have at most 100 characters.");
		assertInvalid(create(this.anaToken, json("ok", "d".repeat(1001))), "/description",
				"The description must have at most 1000 characters.");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   ", " \t\n " })
	void aNameThatIsEmptyAfterTrimmingIsRefused(String name) throws Exception {
		assertInvalid(create(this.anaToken, json(name, null)), "/name", "A name is required.");
		assertThat(count("SELECT count(*) FROM membership WHERE person_id = ?", this.ana.id())).isZero();
	}

	@Test
	void everyInvalidMemberIsNamed() throws Exception {
		ResultActions actions = create(this.anaToken, json(" ", "d".repeat(1001)));

		MvcResult result = assertProblem(actions, 422, "Unprocessable Content", INVALID);
		JsonNode errors = JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("errors");
		assertThat(errors).hasSize(2);
		assertThat(errors.get(0).path("pointer").stringValue()).isEqualTo("/name");
		assertThat(errors.get(1).path("pointer").stringValue()).isEqualTo("/description");
		Contract.assertSatisfies("ValidationProblem", result.getResponse().getContentAsString());
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"name\":null}", "{\"name\":5}", "{\"name\":true}", "{\"name\":[\"x\"]}",
			"{\"name\":\"x\",\"description\":5}", "{\"name\":\"x\",\"owner\":\"someone\"}",
			"{\"name\":\"x\",\"createdBy\":\"someone\"}", "{\"name\":\"x\",\"state\":\"COMPLETED\"}", "[]", "\"x\"",
			"5", "null" })
	void contentThatDoesNotSatisfyTheRequestSchemaIsRefusedWith422(String content) throws Exception {
		MvcResult result = assertProblem(create(this.anaToken, content), 422, "Unprocessable Content", INVALID);

		Contract.assertSatisfies("ValidationProblem", result.getResponse().getContentAsString());
		assertThat(count("SELECT count(*) FROM membership WHERE person_id = ?", this.ana.id())).isZero();
	}

	@Test
	void theOwnerIsNeverTakenFromTheRequest() throws Exception {
		MvcResult result = assertProblem(
				create(this.anaToken, "{\"name\":\"x\",\"ownerId\":\"" + this.rui.id() + "\"}"), 422,
				"Unprocessable Content", INVALID);

		JsonNode error = JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("errors").get(0);
		assertThat(error.path("pointer").stringValue()).isEqualTo("/ownerId");
		assertThat(error.path("detail").stringValue()).isEqualTo("This property is not allowed.");
		assertThat(count("SELECT count(*) FROM membership WHERE person_id = ?", this.ana.id())).isZero();
	}

	@Test
	void duplicateNamesAreAllowed() throws Exception {
		create(this.anaToken, json("Same", null)).andExpect(status().isCreated());
		create(this.anaToken, json("Same", null)).andExpect(status().isCreated());

		assertThat(list(this.anaToken, "").path("totalItems").longValue()).isEqualTo(2);
	}

	@Test
	void malformedContentGets400AndAnotherMediaTypeGets415() throws Exception {
		assertProblem(create(this.anaToken, "{\"name\":"), 400, "Bad Request",
				"The request content is not well-formed JSON.");
		assertProblem(create(this.anaToken, ""), 400, "Bad Request", "The request content is not well-formed JSON.");
		assertProblem(this.mvc.perform(post(PROJECTS).header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(this.anaToken))
			.contentType(MediaType.TEXT_PLAIN)
			.content("{\"name\":\"x\"}")), 415, "Unsupported Media Type",
				"The request content must be in the operation's media type.");
		assertThat(count("SELECT count(*) FROM membership WHERE person_id = ?", this.ana.id())).isZero();
	}

	// View -----------------------------------------------------------------------------------

	@Test
	void aMemberViewsTheProjectWithTheirOwnRole() throws Exception {
		String id = createdId(this.anaToken, "Shared", "About it");
		addMember(id, this.rui, "PROJECT_PARTICIPANT");

		JsonNode asOwner = view(this.anaToken, id);
		JsonNode asParticipant = view(this.ruiToken, id);

		assertThat(asOwner.path("myRole").stringValue()).isEqualTo("PROJECT_OWNER");
		assertThat(asParticipant.path("myRole").stringValue()).isEqualTo("PROJECT_PARTICIPANT");
		assertThat(asParticipant.path("name").stringValue()).isEqualTo("Shared");
		assertThat(asParticipant.path("description").stringValue()).isEqualTo("About it");
	}

	@Test
	void aNonMemberAndAnUnknownProjectAreIndistinguishable() throws Exception {
		String id = createdId(this.anaToken, "Private", null);

		assertProblem(getAs(this.evaToken, PROJECTS + "/" + id), 404, "Not Found", NOT_FOUND);
		assertProblem(getAs(this.evaToken, PROJECTS + "/" + UUID.randomUUID()), 404, "Not Found", NOT_FOUND);
		assertProblem(getAs(this.evaToken, PROJECTS + "/not-a-uuid"), 404, "Not Found", NOT_FOUND);
	}

	@Test
	void aCompletedProjectCanStillBeViewed() throws Exception {
		String id = createdId(this.anaToken, "Done", null);
		this.jdbc.sql("UPDATE project SET state = 'COMPLETED' WHERE project_id = ?").param(UUID.fromString(id)).update();

		assertThat(view(this.anaToken, id).path("state").stringValue()).isEqualTo("COMPLETED");
	}

	// List, paging, search -------------------------------------------------------------------

	@Test
	void theListHoldsOnlyTheProjectsOfTheSignedInPerson() throws Exception {
		createdId(this.anaToken, "Ana's", null);
		String shared = createdId(this.ruiToken, "Rui's, shared with Ana", null);
		addMember(shared, this.ana, "PROJECT_PARTICIPANT");
		createdId(this.evaToken, "Eva's", null);

		JsonNode anas = list(this.anaToken, "");

		assertThat(names(anas)).containsExactly("Ana's", "Rui's, shared with Ana");
		assertThat(anas.path("items").get(1).path("myRole").stringValue()).isEqualTo("PROJECT_PARTICIPANT");
		assertThat(names(list(this.evaToken, ""))).containsExactly("Eva's");
		assertThat(names(list(this.ruiToken, ""))).containsExactly("Rui's, shared with Ana");
	}

	@Test
	void theListIsOrderedByNameIgnoringCaseThenById() throws Exception {
		createdId(this.anaToken, "banana", null);
		createdId(this.anaToken, "Cherry", null);
		createdId(this.anaToken, "Apple", null);

		assertThat(names(list(this.anaToken, ""))).containsExactly("Apple", "banana", "Cherry");
	}

	@Test
	void theListIsPagedWithADefaultOf20AndAMaximumOf100() throws Exception {
		for (int i = 1; i <= 25; i++) {
			createdId(this.anaToken, String.format("P%02d", i), null);
		}

		JsonNode first = list(this.anaToken, "");
		assertThat(first.path("page").intValue()).isEqualTo(1);
		assertThat(first.path("size").intValue()).isEqualTo(20);
		assertThat(first.path("totalItems").longValue()).isEqualTo(25);
		assertThat(first.path("totalPages").longValue()).isEqualTo(2);
		assertThat(names(first)).hasSize(20).startsWith("P01").endsWith("P20");

		JsonNode second = list(this.anaToken, "?page=2");
		assertThat(names(second)).containsExactly("P21", "P22", "P23", "P24", "P25");

		JsonNode small = list(this.anaToken, "?page=3&size=10");
		assertThat(names(small)).containsExactly("P21", "P22", "P23", "P24", "P25");
		assertThat(small.path("totalPages").longValue()).isEqualTo(3);

		assertThat(list(this.anaToken, "?size=100").path("size").intValue()).isEqualTo(100);
	}

	@Test
	void aPagePastTheEndIsEmptyButNotAnError() throws Exception {
		createdId(this.anaToken, "One", null);

		JsonNode page = list(this.anaToken, "?page=9");

		assertThat(page.path("items")).isEmpty();
		assertThat(page.path("totalItems").longValue()).isEqualTo(1);
		assertThat(page.path("totalPages").longValue()).isEqualTo(1);
	}

	@Test
	void anEmptyListHasNoPages() throws Exception {
		JsonNode page = list(this.evaToken, "");

		assertThat(page.path("items")).isEmpty();
		assertThat(page.path("totalItems").longValue()).isZero();
		assertThat(page.path("totalPages").longValue()).isZero();
	}

	@ParameterizedTest
	@ValueSource(strings = { "?page=0", "?page=-1", "?page=x", "?page=", "?page=1.5", "?size=0", "?size=101",
			"?size=x", "?size=", "?name=", "?name=%20%20" })
	void badQueryParametersGet400(String query) throws Exception {
		MvcResult result = this.mvc.perform(withToken(get(java.net.URI.create(PROJECTS + query)), this.anaToken))
			.andExpect(status().isBadRequest())
			.andReturn();

		Contract.assertSatisfies("Problem", result.getResponse().getContentAsString());
	}

	@Test
	void theBadQueryMessagesNameTheParameter() throws Exception {
		assertProblem(this.mvc.perform(withToken(get(java.net.URI.create(PROJECTS + "?size=101")), this.anaToken)), 400, "Bad Request",
				"The query parameter size must be an integer from 1 to 100.");
		assertProblem(this.mvc.perform(withToken(get(java.net.URI.create(PROJECTS + "?page=0")), this.anaToken)), 400, "Bad Request",
				"The query parameter page must be an integer of at least 1.");
		assertProblem(this.mvc.perform(withToken(get(java.net.URI.create(PROJECTS + "?name=")), this.anaToken)), 400, "Bad Request",
				"The query parameter name must not be empty.");
	}

	@Test
	void searchFindsTheTextInTheNameIgnoringCaseButNotAccentsAndItIsPaged() throws Exception {
		createdId(this.anaToken, "Website Redesign", "mentions Café here");
		createdId(this.anaToken, "Website Audit", null);
		createdId(this.anaToken, "Mobile App", null);
		createdId(this.anaToken, "Café", null);

		assertThat(names(list(this.anaToken, "?name=WEB"))).containsExactly("Website Audit", "Website Redesign");
		assertThat(names(list(this.anaToken, "?name=site%20a"))).containsExactly("Website Audit");
		assertThat(names(list(this.anaToken, "?name=%20%20app%20"))).containsExactly("Mobile App");
		assertThat(names(list(this.anaToken, "?name=cafe"))).isEmpty();
		assertThat(names(list(this.anaToken, "?name=caf%C3%A9"))).containsExactly("Café");
		assertThat(names(list(this.anaToken, "?name=here"))).isEmpty();
		JsonNode paged = list(this.anaToken, "?name=website&size=1&page=2");
		assertThat(names(paged)).containsExactly("Website Redesign");
		assertThat(paged.path("totalItems").longValue()).isEqualTo(2);
		assertThat(paged.path("totalPages").longValue()).isEqualTo(2);
	}

	@Test
	void searchTreatsPercentUnderscoreAndTheEscapeCharacterAsPlainText() throws Exception {
		createdId(this.anaToken, "100% done", null);
		createdId(this.anaToken, "a_b", null);
		createdId(this.anaToken, "axb", null);
		createdId(this.anaToken, "Wow!", null);

		assertThat(names(list(this.anaToken, "?name=%25"))).containsExactly("100% done");
		assertThat(names(list(this.anaToken, "?name=a_b"))).containsExactly("a_b");
		assertThat(names(list(this.anaToken, "?name=_"))).containsExactly("a_b");
		assertThat(names(list(this.anaToken, "?name=!"))).containsExactly("Wow!");
	}

	@Test
	void searchNeverShowsAProjectOfSomeoneElse() throws Exception {
		createdId(this.anaToken, "Secret plan", null);

		assertThat(names(list(this.evaToken, "?name=secret"))).isEmpty();
	}

	// Edit -----------------------------------------------------------------------------------

	@Test
	void theProjectOwnerChangesTheNameAndTheDescription() throws Exception {
		String id = createdId(this.anaToken, "Old", "Old text");

		MvcResult result = edit(this.anaToken, id, "{\"name\":\"  New  \",\"description\":\" New text \"}")
			.andExpect(status().isOk())
			.andReturn();

		Contract.assertSatisfies("Project", result.getResponse().getContentAsString());
		JsonNode project = view(this.anaToken, id);
		assertThat(project.path("name").stringValue()).isEqualTo("New");
		assertThat(project.path("description").stringValue()).isEqualTo("New text");
		assertThat(project.path("state").stringValue()).isEqualTo("ACTIVE");
	}

	@Test
	void anAbsentMemberIsLeftUnchangedAndANullDescriptionRemovesIt() throws Exception {
		String id = createdId(this.anaToken, "Keep", "Remove me");

		edit(this.anaToken, id, "{\"name\":\"Kept\"}").andExpect(status().isOk());
		assertThat(view(this.anaToken, id).path("description").stringValue()).isEqualTo("Remove me");

		edit(this.anaToken, id, "{\"description\":null}").andExpect(status().isOk());
		JsonNode project = view(this.anaToken, id);
		assertThat(project.path("name").stringValue()).isEqualTo("Kept");
		assertThat(project.has("description")).isFalse();

		edit(this.anaToken, id, "{}").andExpect(status().isOk());
		assertThat(view(this.anaToken, id).path("name").stringValue()).isEqualTo("Kept");
	}

	@Test
	void aProjectParticipantCannotEditAndNothingChanges() throws Exception {
		String id = createdId(this.anaToken, "Mine", "Text");
		addMember(id, this.rui, "PROJECT_PARTICIPANT");

		assertProblem(edit(this.ruiToken, id, "{\"name\":\"Hijacked\"}"), 403, "Forbidden", NOT_OWNER);

		assertThat(view(this.anaToken, id).path("name").stringValue()).isEqualTo("Mine");
	}

	@Test
	void aNonMemberCannotEditAndNothingChanges() throws Exception {
		String id = createdId(this.anaToken, "Mine", null);

		assertProblem(edit(this.evaToken, id, "{\"name\":\"Hijacked\"}"), 404, "Not Found", NOT_FOUND);

		assertThat(view(this.anaToken, id).path("name").stringValue()).isEqualTo("Mine");
	}

	@Test
	void invalidContentIsRefusedWithoutChangingTheProject() throws Exception {
		String id = createdId(this.anaToken, "Stable", "Text");

		assertInvalid(edit(this.anaToken, id, "{\"name\":\" \"}"), "/name", "A name is required.");
		assertInvalid(edit(this.anaToken, id, "{\"name\":null}"), "/name", "The name cannot be removed.");
		assertInvalid(edit(this.anaToken, id, "{\"name\":5}"), "/name", "The name must be a string.");
		assertInvalid(edit(this.anaToken, id, "{\"description\":\"" + "d".repeat(1001) + "\"}"), "/description",
				"The description must have at most 1000 characters.");
		assertInvalid(edit(this.anaToken, id, "{\"state\":\"COMPLETED\"}"), "/state", "This property is not allowed.");
		assertProblem(edit(this.anaToken, id, "[]"), 422, "Unprocessable Content", INVALID);

		JsonNode project = view(this.anaToken, id);
		assertThat(project.path("name").stringValue()).isEqualTo("Stable");
		assertThat(project.path("description").stringValue()).isEqualTo("Text");
		assertThat(project.path("state").stringValue()).isEqualTo("ACTIVE");
	}

	@Test
	void anEditChecksAccessBeforeTheMediaTypeAndTheContent() throws Exception {
		String id = createdId(this.anaToken, "Mine", null);
		addMember(id, this.rui, "PROJECT_PARTICIPANT");

		assertProblem(patchWith(this.evaToken, id, MediaType.TEXT_PLAIN, "not json"), 404, "Not Found", NOT_FOUND);
		assertProblem(patchWith(this.ruiToken, id, MediaType.TEXT_PLAIN, "not json"), 403, "Forbidden", NOT_OWNER);
		assertProblem(patchWith(this.ruiToken, id, MERGE_PATCH, "{not json"), 403, "Forbidden", NOT_OWNER);
		assertProblem(patchWith(this.anaToken, id, MediaType.APPLICATION_JSON, "{}"), 415, "Unsupported Media Type",
				"The request content must be in the operation's media type.");
		assertProblem(patchWith(this.anaToken, id, MERGE_PATCH, "{not json"), 400, "Bad Request",
				"The request content is not well-formed JSON.");
		assertProblem(patchWith(this.anaToken, id, MERGE_PATCH, ""), 400, "Bad Request",
				"The request content is not well-formed JSON.");
	}

	@Test
	void aCompletedProjectCannotBeEditedButInvalidContentIsStillReportedFirst() throws Exception {
		String id = createdId(this.anaToken, "Closed", null);
		complete(id);

		assertProblem(edit(this.anaToken, id, "{\"name\":\"Other\"}"), 409, "Conflict", COMPLETED);
		assertInvalid(edit(this.anaToken, id, "{\"name\":\" \"}"), "/name", "A name is required.");
		assertThat(view(this.anaToken, id).path("name").stringValue()).isEqualTo("Closed");
	}

	// Delete ---------------------------------------------------------------------------------

	@Test
	void theProjectOwnerDeletesAnActiveProjectAndItDisappearsEverywhere() throws Exception {
		String id = createdId(this.anaToken, "Doomed", null);
		addMember(id, this.rui, "PROJECT_PARTICIPANT");
		UUID task = addTaskWithAssignmentAndComment(id, this.rui);

		this.mvc.perform(withToken(delete(PROJECTS + "/" + id), this.anaToken))
			.andExpect(status().isNoContent())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(""));

		assertProblem(getAs(this.anaToken, PROJECTS + "/" + id), 404, "Not Found", NOT_FOUND);
		assertProblem(getAs(this.ruiToken, PROJECTS + "/" + id), 404, "Not Found", NOT_FOUND);
		assertProblem(edit(this.anaToken, id, "{\"name\":\"x\"}"), 404, "Not Found", NOT_FOUND);
		assertProblem(deleteRequest(this.anaToken, id), 404, "Not Found", NOT_FOUND);
		assertThat(names(list(this.anaToken, ""))).isEmpty();
		assertThat(names(list(this.ruiToken, ""))).isEmpty();
		assertThat(names(list(this.anaToken, "?name=doomed"))).isEmpty();
		// Soft deletion: the project and everything that depends on it stay in storage, so no record
		// is orphaned, and the project is marked as deleted (docs/database.md, rule 6).
		assertThat(this.jdbc.sql("SELECT count(*) FROM project WHERE project_id = ? AND deleted_at IS NOT NULL")
			.param(UUID.fromString(id))
			.query(Integer.class)
			.single()).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM membership WHERE project_id = ?", UUID.fromString(id))).isEqualTo(2);
		assertThat(count("SELECT count(*) FROM task WHERE project_id = ?", UUID.fromString(id))).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM assignment WHERE task_id = ?", task)).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM comment WHERE task_id = ?", task)).isEqualTo(1);
		assertNoOrphans();
	}

	@Test
	void aProjectParticipantCannotDelete() throws Exception {
		String id = createdId(this.anaToken, "Mine", null);
		addMember(id, this.rui, "PROJECT_PARTICIPANT");

		assertProblem(deleteRequest(this.ruiToken, id), 403, "Forbidden", NOT_OWNER);

		assertThat(view(this.anaToken, id).path("name").stringValue()).isEqualTo("Mine");
	}

	@Test
	void aNonMemberCannotDeleteAndTheProjectStays() throws Exception {
		String id = createdId(this.anaToken, "Mine", null);

		assertProblem(deleteRequest(this.evaToken, id), 404, "Not Found", NOT_FOUND);

		assertThat(view(this.anaToken, id).path("name").stringValue()).isEqualTo("Mine");
	}

	@Test
	void aCompletedProjectCannotBeDeleted() throws Exception {
		String id = createdId(this.anaToken, "Closed", null);
		complete(id);

		assertProblem(deleteRequest(this.anaToken, id), 409, "Conflict", COMPLETED);

		assertThat(view(this.anaToken, id).path("state").stringValue()).isEqualTo("COMPLETED");
	}

	@Test
	void aDeletedProjectStaysDeletedWhenTheNameIsReused() throws Exception {
		String id = createdId(this.anaToken, "Again", null);
		this.mvc.perform(withToken(delete(PROJECTS + "/" + id), this.anaToken)).andExpect(status().isNoContent());

		String second = createdId(this.anaToken, "Again", null);

		assertThat(second).isNotEqualTo(id);
		assertThat(names(list(this.anaToken, ""))).containsExactly("Again");
	}

	// Authentication -------------------------------------------------------------------------

	@Test
	void everyProjectOperationNeedsAToken() throws Exception {
		String id = createdId(this.anaToken, "Mine", null);

		TestAccess.assertNoToken(this.mvc.perform(post(PROJECTS).contentType(MediaType.APPLICATION_JSON).content("{}")));
		TestAccess.assertNoToken(this.mvc.perform(get(PROJECTS)));
		TestAccess.assertNoToken(this.mvc.perform(get(PROJECTS + "/" + id)));
		TestAccess.assertNoToken(this.mvc.perform(patch(PROJECTS + "/" + id).contentType(MERGE_PATCH).content("{}")));
		TestAccess.assertNoToken(this.mvc.perform(delete(PROJECTS + "/" + id)));
		assertThat(view(this.anaToken, id).path("name").stringValue()).isEqualTo("Mine");
	}

	@Test
	void aTokenEndedBySigningOutIsRefused() throws Exception {
		String token = TestAccess.accessToken(this.mvc, this.ana);
		TestAccess.signOut(this.mvc, token);

		TestAccess.assertInvalidToken(this.mvc.perform(withToken(get(PROJECTS), token)));
	}

	@Test
	void aProjectStaysAfterSigningOutAndInAgain() throws Exception {
		String id = createdId(this.anaToken, "Lasting", null);
		TestAccess.signOut(this.mvc, this.anaToken);

		String again = TestAccess.accessToken(this.mvc, this.ana);

		assertThat(view(again, id).path("name").stringValue()).isEqualTo("Lasting");
		assertThat(names(list(again, ""))).containsExactly("Lasting");
	}

	@Test
	void responsesSatisfyTheContract() throws Exception {
		createdId(this.anaToken, "Contract", "Described");
		createdId(this.anaToken, "Contract two", null);

		MvcResult result = this.mvc.perform(withToken(get(PROJECTS), this.anaToken)).andExpect(status().isOk()).andReturn();

		Contract.assertSatisfies("ProjectPage", result.getResponse().getContentAsString());
	}

	// Helpers --------------------------------------------------------------------------------

	private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
		return request.header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(token));
	}

	private ResultActions create(String token, String content) throws Exception {
		return this.mvc.perform(withToken(post(PROJECTS), token).contentType(MediaType.APPLICATION_JSON).content(content));
	}

	private ResultActions edit(String token, String id, String content) throws Exception {
		return patchWith(token, id, MERGE_PATCH, content);
	}

	private ResultActions patchWith(String token, String id, MediaType type, String content) throws Exception {
		return this.mvc.perform(withToken(patch(PROJECTS + "/" + id), token).contentType(type).content(content));
	}

	private ResultActions getAs(String token, String url) throws Exception {
		return this.mvc.perform(withToken(get(url), token));
	}

	private ResultActions deleteRequest(String token, String id) throws Exception {
		return this.mvc.perform(withToken(delete(PROJECTS + "/" + id), token));
	}

	private static String json(String name, String description) {
		return JsonMapper.shared().writeValueAsString(description == null
				? java.util.Map.of("name", name) : java.util.Map.of("name", name, "description", description));
	}

	private String createdId(String token, String name, String description) throws Exception {
		return idOf(create(token, json(name, description)).andExpect(status().isCreated()).andReturn());
	}

	private static String idOf(MvcResult result) throws Exception {
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("projectId").stringValue();
	}

	private JsonNode view(String token, String id) throws Exception {
		MvcResult result = getAs(token, PROJECTS + "/" + id).andExpect(status().isOk()).andReturn();
		Contract.assertSatisfies("Project", result.getResponse().getContentAsString());
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString());
	}

	private JsonNode list(String token, String query) throws Exception {
		MvcResult result = this.mvc.perform(withToken(get(java.net.URI.create(PROJECTS + query)), token)).andExpect(status().isOk()).andReturn();
		Contract.assertSatisfies("ProjectPage", result.getResponse().getContentAsString());
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString());
	}

	private static List<String> names(JsonNode page) {
		return page.path("items").valueStream().map((item) -> item.path("name").stringValue()).toList();
	}

	private void assertInvalid(ResultActions actions, String pointer, String detail) throws Exception {
		MvcResult result = assertProblem(actions, 422, "Unprocessable Content", INVALID);
		JsonNode errors = JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path("errors");
		assertThat(errors.valueStream()
			.anyMatch((error) -> pointer.equals(error.path("pointer").stringValue())
					&& detail.equals(error.path("detail").stringValue()))).as("errors %s", errors).isTrue();
	}

	private void addMember(String projectId, TestAccess.TestPerson person, String role) {
		this.jdbc.sql("INSERT INTO membership (project_id, person_id, role) VALUES (?, ?, ?)")
			.params(UUID.fromString(projectId), person.id(), role)
			.update();
	}

	private void complete(String projectId) {
		this.jdbc.sql("UPDATE project SET state = 'COMPLETED' WHERE project_id = ?")
			.param(UUID.fromString(projectId))
			.update();
	}

	private UUID addTaskWithAssignmentAndComment(String projectId, TestAccess.TestPerson assignee) {
		UUID task = UUID.randomUUID();
		this.jdbc.sql("INSERT INTO task (task_id, project_id, title) VALUES (?, ?, 'A task')")
			.params(task, UUID.fromString(projectId))
			.update();
		this.jdbc.sql("INSERT INTO assignment (task_id, person_id, project_id) VALUES (?, ?, ?)")
			.params(task, assignee.id(), UUID.fromString(projectId))
			.update();
		this.jdbc.sql("INSERT INTO comment (comment_id, task_id, author_person_id, text, created_at) "
				+ "VALUES (?, ?, ?, 'A comment', now())")
			.params(UUID.randomUUID(), task, assignee.id())
			.update();
		return task;
	}

	private void assertNoOrphans() {
		assertThat(count("SELECT count(*) FROM membership m WHERE NOT EXISTS (SELECT 1 FROM project p WHERE p.project_id = m.project_id)")).isZero();
		assertThat(count("SELECT count(*) FROM task t WHERE NOT EXISTS (SELECT 1 FROM project p WHERE p.project_id = t.project_id)")).isZero();
		assertThat(count("SELECT count(*) FROM assignment a WHERE NOT EXISTS (SELECT 1 FROM task t WHERE t.task_id = a.task_id)")).isZero();
		assertThat(count("SELECT count(*) FROM comment c WHERE NOT EXISTS (SELECT 1 FROM task t WHERE t.task_id = c.task_id)")).isZero();
	}

	private int count(String sql, Object... parameters) {
		return this.jdbc.sql(sql).params(parameters).query(Integer.class).single();
	}

}
