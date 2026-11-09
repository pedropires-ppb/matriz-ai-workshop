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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static com.matriz.Contract.assertProblem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The list of the members of a project through the API (openapi.yaml, {@code listProjectMembers};
 * US-026; EX-003, EX-012; Assumptions A-027 and A-034), with real persistence. Ana is the Project
 * Owner, Rui and Eva are Project Participants, and Daniel is a member of no project.
 */
@AutoConfigureMockMvc
class MembersApiTests extends DatabaseTestSupport {

	private static final String PROJECT_NOT_FOUND = "The project does not exist or is not accessible.";

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

	private String danielToken;

	private String project;

	@BeforeEach
	void addPeopleAndAProject() throws Exception {
		this.ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		this.rui = TestAccess.addPerson(this.jdbc, "rui Martins");
		this.eva = TestAccess.addPerson(this.jdbc, "Eva Santos");
		this.daniel = TestAccess.addPerson(this.jdbc, "Daniel Rocha");
		this.anaToken = TestAccess.accessToken(this.mvc, this.ana);
		this.ruiToken = TestAccess.accessToken(this.mvc, this.rui);
		this.danielToken = TestAccess.accessToken(this.mvc, this.daniel);
		MvcResult result = this.mvc
			.perform(post("/api/projects").header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(this.anaToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Website Redesign\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		this.project = JsonMapper.shared()
			.readTree(result.getResponse().getContentAsString())
			.path("projectId")
			.stringValue();
		addParticipant(this.rui);
		addParticipant(this.eva);
	}

	@Test
	void aMemberSeesTheOwnerFirstAndThenTheOthersByNameIgnoringLetterCase() throws Exception {
		for (String token : List.of(this.anaToken, this.ruiToken)) {
			MvcResult result = members(token, this.project).andExpect(status().isOk()).andReturn();

			String body = result.getResponse().getContentAsString();
			JsonNode members = JsonMapper.shared().readTree(body);
			assertThat(members.isArray()).isTrue();
			Contract.assertSatisfies("Member", members.get(0).toString());
			Contract.assertSatisfies("Member", members.get(1).toString());
			Contract.assertSatisfies("Member", members.get(2).toString());
			assertThat(members).hasSize(3);
			assertThat(members.get(0).path("person").path("displayName").stringValue()).isEqualTo("Ana Silva");
			assertThat(members.get(0).path("person").path("personId").stringValue())
				.isEqualTo(this.ana.id().toString());
			assertThat(members.get(0).path("role").stringValue()).isEqualTo("PROJECT_OWNER");
			assertThat(members.get(1).path("person").path("displayName").stringValue()).isEqualTo("Eva Santos");
			assertThat(members.get(1).path("role").stringValue()).isEqualTo("PROJECT_PARTICIPANT");
			assertThat(members.get(2).path("person").path("displayName").stringValue()).isEqualTo("rui Martins");
		}
	}

	@Test
	void noLoginNameOrPasswordHashIsShown() throws Exception {
		String body = members(this.anaToken, this.project).andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain(this.ana.loginName(), this.rui.loginName(), "passwordHash", "loginName");
	}

	@Test
	void aCompletedProjectStillShowsItsMembers() throws Exception {
		this.jdbc.sql("UPDATE project SET state = 'COMPLETED' WHERE project_id = ?")
			.param(UUID.fromString(this.project))
			.update();

		members(this.ruiToken, this.project).andExpect(status().isOk());
	}

	@Test
	void aPersonWhoIsNotAMemberIsToldTheProjectDoesNotExist() throws Exception {
		assertProblem(members(this.danielToken, this.project), 404, "Not Found", PROJECT_NOT_FOUND);
	}

	@Test
	void aDeletedOrUnknownOrMalformedProjectIsNotFound() throws Exception {
		this.jdbc.sql("UPDATE project SET deleted_at = now() WHERE project_id = ?")
			.param(UUID.fromString(this.project))
			.update();

		assertProblem(members(this.anaToken, this.project), 404, "Not Found", PROJECT_NOT_FOUND);
		assertProblem(members(this.anaToken, UUID.randomUUID().toString()), 404, "Not Found", PROJECT_NOT_FOUND);
		assertProblem(members(this.anaToken, "not-a-uuid"), 404, "Not Found", PROJECT_NOT_FOUND);
	}

	@Test
	void aRequestWithoutATokenIsRefused() throws Exception {
		TestAccess.assertNoToken(this.mvc.perform(get("/api/projects/" + this.project + "/members")));
	}

	@ParameterizedTest
	@ValueSource(strings = { "PUT", "DELETE" })
	void theMembersAreNotChangedAsAWhole(String method) throws Exception {
		this.mvc
			.perform(request(HttpMethod.valueOf(method), "/api/projects/" + this.project + "/members")
				.header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(this.anaToken)))
			.andExpect(status().isMethodNotAllowed());
	}

	private ResultActions members(String token, String project) throws Exception {
		return this.mvc.perform(get("/api/projects/" + project + "/members").header(HttpHeaders.AUTHORIZATION,
				TestAccess.bearer(token)));
	}

	private void addParticipant(TestAccess.TestPerson person) {
		this.jdbc.sql("INSERT INTO membership (project_id, person_id, role) VALUES (?, ?, 'PROJECT_PARTICIPANT')")
			.params(UUID.fromString(this.project), person.id())
			.update();
	}

}
