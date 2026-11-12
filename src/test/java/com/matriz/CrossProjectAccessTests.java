package com.matriz;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Changing the identifiers of a request does not give access (NFR-002; EX-003; Assumption A-022): a
 * task, a comment, an assignment, or a member of one project,
 * named through the path of another project, is answered like a resource that does not exist, whoever
 * asks and whatever the person's role in either project, and nothing changes. Ana owns project A and
 * Eva owns project B; Rui is a Project Participant of both, and Ana's task is assigned to him.
 */
@AutoConfigureMockMvc
class CrossProjectAccessTests extends DatabaseTestSupport {

	private static final String TASK_NOT_FOUND = "The project or task does not exist or is not accessible.";

	private static final String PATCH_TYPE = "application/merge-patch+json";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcClient jdbc;

	private TestAccess.TestPerson ana;

	private TestAccess.TestPerson eva;

	private TestAccess.TestPerson rui;

	private String anaToken;

	private String evaToken;

	private String ruiToken;

	private String projectA;

	private String projectB;

	private String taskA;

	private String taskB;

	@BeforeEach
	void addTwoProjectsEachWithATaskAndAComment() throws Exception {
		this.ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		this.eva = TestAccess.addPerson(this.jdbc, "Eva Costa");
		this.rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		this.anaToken = TestAccess.accessToken(this.mvc, this.ana);
		this.evaToken = TestAccess.accessToken(this.mvc, this.eva);
		this.ruiToken = TestAccess.accessToken(this.mvc, this.rui);
		this.projectA = created(this.anaToken, "/api/projects", "{\"name\":\"Project A\"}", "projectId");
		this.projectB = created(this.evaToken, "/api/projects", "{\"name\":\"Project B\"}", "projectId");
		for (String project : List.of(this.projectA, this.projectB)) {
			this.jdbc.sql("INSERT INTO membership (project_id, person_id, role) VALUES (?, ?, 'PROJECT_PARTICIPANT')")
				.params(UUID.fromString(project), this.rui.id())
				.update();
		}
		this.taskA = created(this.anaToken, "/api/projects/" + this.projectA + "/tasks", "{\"title\":\"Task of A\"}",
				"taskId");
		this.taskB = created(this.evaToken, "/api/projects/" + this.projectB + "/tasks", "{\"title\":\"Task of B\"}",
				"taskId");
		created(this.anaToken, "/api/projects/" + this.projectA + "/tasks/" + this.taskA + "/comments",
				"{\"text\":\"Comment of A\"}", "commentId");
		created(this.evaToken, "/api/projects/" + this.projectB + "/tasks/" + this.taskB + "/comments",
				"{\"text\":\"Comment of B\"}", "commentId");
		send(this.anaToken, HttpMethod.POST, "/api/projects/" + this.projectA + "/tasks/" + this.taskA + "/assignees",
				"{\"personId\":\"" + this.rui.id() + "\"}", MediaType.APPLICATION_JSON_VALUE);
	}

	@Test
	void aTaskOfOneProjectNamedThroughAnotherIsNotFoundForEveryOperationAndEveryPerson() throws Exception {
		String before = snapshot();
		List<String> answered = new ArrayList<>();

		// Each row: who asks, the project of the path, the task of the path. In every row the person is no member
		// of the project of the path, or the task does not belong to it.
		String[][] attempts = { { "ana", "B", "A" }, { "ana", "A", "B" }, { "eva", "A", "A" }, { "eva", "A", "B" },
				{ "eva", "B", "A" }, { "rui", "A", "B" }, { "rui", "B", "A" } };
		for (String[] attempt : attempts) {
			String token = token(attempt[0]);
			String project = "A".equals(attempt[1]) ? this.projectA : this.projectB;
			String task = "A".equals(attempt[2]) ? this.taskA : this.taskB;
			for (Operation operation : taskOperations(project, task, this.rui.id())) {
				MvcResult result = send(token, operation.method(), operation.path(), operation.body(),
						operation.contentType());
				String where = attempt[0] + " " + operation.method() + " " + operation.path();
				assertThat(result.getResponse().getStatus()).as(where + " -> " + result.getResponse().getContentAsString())
					.isEqualTo(404);
				assertThat(detail(result)).as(where).isEqualTo(TASK_NOT_FOUND);
				answered.add(where);
			}
		}

		assertThat(answered).hasSize(attempts.length * 8);
		assertThat(snapshot()).isEqualTo(before);
	}

	@Test
	void aMemberOfOneProjectNamedThroughAnotherIsAnsweredLikeAPersonWhoIsNoMember() throws Exception {
		String before = snapshot();
		String unknown = UUID.randomUUID().toString();
		String memberNotFound = detail(send(this.anaToken, HttpMethod.DELETE,
				"/api/projects/" + this.projectA + "/members/" + unknown, null, null));

		for (String[] attempt : new String[][] { { "ana", this.projectA, this.eva.id().toString() },
				{ "ana", this.projectB, this.rui.id().toString() }, { "eva", this.projectB, this.ana.id().toString() },
				{ "eva", this.projectA, this.rui.id().toString() } }) {
			MvcResult result = send(token(attempt[0]), HttpMethod.DELETE,
					"/api/projects/" + attempt[1] + "/members/" + attempt[2], null, null);
			assertThat(result.getResponse().getStatus()).as(attempt[0] + " removes " + attempt[2]).isEqualTo(404);
			assertThat(detail(result)).isEqualTo(memberNotFound);
		}
		for (String[] attempt : new String[][] { { "eva", this.projectA }, { "ana", this.projectB } }) {
			String person = "eva".equals(attempt[0]) ? this.eva.id().toString() : this.ana.id().toString();
			String base = "/api/projects/" + attempt[1];
			assertThat(send(token(attempt[0]), HttpMethod.POST, base + "/members", "{\"personId\":\"" + person + "\"}",
					MediaType.APPLICATION_JSON_VALUE).getResponse().getStatus()).isEqualTo(404);
			assertThat(send(token(attempt[0]), HttpMethod.GET, base + "/members", null, null).getResponse().getStatus())
				.isEqualTo(404);
			assertThat(send(token(attempt[0]), HttpMethod.GET, base + "/eligible-participants", null, null)
				.getResponse()
				.getStatus()).isEqualTo(404);
		}

		assertThat(snapshot()).isEqualTo(before);
	}

	private List<Operation> taskOperations(String project, String task, UUID person) {
		String base = "/api/projects/" + project + "/tasks/" + task;
		return List.of(new Operation(HttpMethod.GET, base, null, null),
				new Operation(HttpMethod.PATCH, base, "{\"title\":\"Hacked\"}", PATCH_TYPE),
				new Operation(HttpMethod.PUT, base + "/state", "{\"state\":\"DONE\"}", MediaType.APPLICATION_JSON_VALUE),
				new Operation(HttpMethod.DELETE, base, null, null),
				new Operation(HttpMethod.GET, base + "/comments", null, null),
				new Operation(HttpMethod.POST, base + "/comments", "{\"text\":\"Hacked\"}",
						MediaType.APPLICATION_JSON_VALUE),
				new Operation(HttpMethod.POST, base + "/assignees", "{\"personId\":\"" + person + "\"}",
						MediaType.APPLICATION_JSON_VALUE),
				new Operation(HttpMethod.DELETE, base + "/assignees/" + person, null, null));
	}

	private String token(String who) {
		return switch (who) {
			case "ana" -> this.anaToken;
			case "eva" -> this.evaToken;
			default -> this.ruiToken;
		};
	}

	private MvcResult send(String token, HttpMethod method, String path, String body, String contentType)
			throws Exception {
		var builder = request(method, path).header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(token));
		if (contentType != null) {
			builder = builder.contentType(contentType);
		}
		if (body != null) {
			builder = builder.content(body);
		}
		return this.mvc.perform(builder).andReturn();
	}

	private String created(String token, String path, String body, String property) throws Exception {
		MvcResult result = send(token, HttpMethod.POST, path, body, MediaType.APPLICATION_JSON_VALUE);
		assertThat(result.getResponse().getStatus()).as(path).isEqualTo(201);
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString()).path(property).stringValue();
	}

	private static String detail(MvcResult result) throws Exception {
		JsonNode problem = JsonMapper.shared().readTree(result.getResponse().getContentAsString());
		return problem.path("detail").stringValue();
	}

	/** A fingerprint of every row of the tables that hold the data of projects, so that any change shows. */
	private String snapshot() {
		StringBuilder fingerprint = new StringBuilder();
		for (String table : List.of("project", "task", "comment", "membership", "assignment")) {
			fingerprint.append(table)
				.append('=')
				.append(this.jdbc
					.sql("SELECT coalesce(md5(string_agg(t::text, '|' ORDER BY t::text)), '') FROM " + table + " t")
					.query(String.class)
					.single())
				.append(';');
		}
		return fingerprint.toString();
	}

	private record Operation(HttpMethod method, String path, String body, String contentType) {

	}

}
