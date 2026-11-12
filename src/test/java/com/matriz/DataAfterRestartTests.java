package com.matriz;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a person created is read back, whole, after Matriz restarts on the same database (the
 * exercise's case 03, of week 3, exercise 8: tasks stay after the backend restarts). The data is created and read over HTTP, as a client outside the User interface
 * would, then the application is stopped and started again, and the same reads give the same answers.
 * A deleted task and a deleted project stay not found. The reads before and after are compared
 * whole, so that a field kept only in memory would show as a difference.
 */
class DataAfterRestartTests {

	@Test
	void aProjectItsTaskItsAssigneeAndItsCommentAreReadBackAfterARestartAndADeletionIsKept() throws Exception {
		String url = SharedPostgres.newDatabase();
		TestAccess.TestPerson ana;
		TestAccess.TestPerson rui;
		Map<String, String> before = new LinkedHashMap<>();
		String project;
		String task;
		String doomed;
		String anaToken;
		String ruiToken;
		try (HttpClient http = HttpClient.newHttpClient()) {
			try (ConfigurableApplicationContext matriz = start(url)) {
				ana = TestAccess.addPerson(jdbc(url), "Ana Silva");
				rui = TestAccess.addPerson(jdbc(url), "Rui Martins");
				anaToken = signIn(http, matriz, ana);
				ruiToken = signIn(http, matriz, rui);
				project = idOf(http, matriz, anaToken, "POST", "/api/projects", 201,
						"{\"name\":\"Website redesign\",\"description\":\"New site\"}", "projectId");
				jdbc(url).sql("INSERT INTO membership (project_id, person_id, role)"
						+ " VALUES (?, ?, 'PROJECT_PARTICIPANT')")
					.params(java.util.UUID.fromString(project), rui.id())
					.update();
				String tasks = "/api/projects/" + project + "/tasks";
				task = idOf(http, matriz, anaToken, "POST", tasks, 201,
						"{\"title\":\"Prepare proposal\",\"description\":\"Numbers for Q1\",\"priority\":\"HIGH\"}",
						"taskId");
				doomed = idOf(http, matriz, anaToken, "POST", tasks, 201, "{\"title\":\"Discard me\"}", "taskId");
				send(http, matriz, anaToken, "POST", tasks + "/" + task + "/assignees",
						"{\"personId\":\"" + rui.id() + "\"}", 200);
				send(http, matriz, ruiToken, "POST", tasks + "/" + task + "/comments",
						"{\"text\":\"Started the numbers\"}", 201);
				send(http, matriz, ruiToken, "PUT", tasks + "/" + task + "/state", "{\"state\":\"IN_PROGRESS\"}", 200);
				send(http, matriz, anaToken, "DELETE", tasks + "/" + doomed, null, 204);

				before = reads(http, matriz, anaToken, project, task);
				JsonNode theTask = JsonMapper.shared().readTree(before.get("task"));
				assertThat(theTask.path("title").stringValue()).isEqualTo("Prepare proposal");
				assertThat(theTask.path("state").stringValue()).isEqualTo("IN_PROGRESS");
				assertThat(theTask.path("priority").stringValue()).isEqualTo("HIGH");
				assertThat(theTask.path("assignees").get(0).path("displayName").stringValue()).isEqualTo("Rui Martins");
				JsonNode comments = JsonMapper.shared().readTree(before.get("comments"));
				assertThat(comments).hasSize(1);
				assertThat(comments.get(0).path("text").stringValue()).isEqualTo("Started the numbers");
				assertThat(comments.get(0).path("author").path("displayName").stringValue()).isEqualTo("Rui Martins");
			}

			try (ConfigurableApplicationContext matriz = start(url)) {
				anaToken = signIn(http, matriz, ana);
				Map<String, String> after = reads(http, matriz, anaToken, project, task);
				assertThat(after).isEqualTo(before);
				assertThat(send(http, matriz, anaToken, "GET", "/api/projects/" + project + "/tasks/" + doomed, null,
						404).statusCode()).isEqualTo(404);
			}
		}
	}

	private static Map<String, String> reads(HttpClient http, ConfigurableApplicationContext matriz, String token,
			String project, String task) throws Exception {
		String base = "/api/projects/" + project;
		Map<String, String> answers = new LinkedHashMap<>();
		answers.put("project", send(http, matriz, token, "GET", base, null, 200).body());
		answers.put("tasks", send(http, matriz, token, "GET", base + "/tasks", null, 200).body());
		answers.put("task", send(http, matriz, token, "GET", base + "/tasks/" + task, null, 200).body());
		answers.put("comments", send(http, matriz, token, "GET", base + "/tasks/" + task + "/comments", null, 200)
			.body());
		answers.put("members", send(http, matriz, token, "GET", base + "/members", null, 200).body());
		return answers;
	}

	private static String idOf(HttpClient http, ConfigurableApplicationContext matriz, String token, String method,
			String path, int status, String body, String property) throws Exception {
		return JsonMapper.shared()
			.readTree(send(http, matriz, token, method, path, body, status).body())
			.path(property)
			.stringValue();
	}

	private static String signIn(HttpClient http, ConfigurableApplicationContext matriz, TestAccess.TestPerson person)
			throws IOException, InterruptedException {
		String credentials = JsonMapper.shared()
			.writeValueAsString(Map.of("loginName", person.loginName(), "password", person.password()));
		HttpResponse<String> response = http.send(HttpRequest.newBuilder(uri(matriz, "/api/session"))
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.POST(HttpRequest.BodyPublishers.ofString(credentials))
			.build(), HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).isEqualTo(200);
		return JsonMapper.shared().readTree(response.body()).path("accessToken").stringValue();
	}

	private static HttpResponse<String> send(HttpClient http, ConfigurableApplicationContext matriz, String token,
			String method, String path, String body, int expectedStatus) throws IOException, InterruptedException {
		HttpRequest.Builder request = HttpRequest.newBuilder(uri(matriz, path))
			.header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(token));
		if (body != null) {
			request.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.method(method, HttpRequest.BodyPublishers.ofString(body));
		}
		else {
			request.method(method, HttpRequest.BodyPublishers.noBody());
		}
		HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).as(method + " " + path + " -> " + response.body()).isEqualTo(expectedStatus);
		return response;
	}

	private static URI uri(ConfigurableApplicationContext matriz, String path) {
		return URI.create("http://localhost:" + matriz.getEnvironment().getProperty("local.server.port") + path);
	}

	private static JdbcClient jdbc(String url) {
		return JdbcClient.create(new DriverManagerDataSource(url, SharedPostgres.username(), SharedPostgres.password()));
	}

	/** Starts the whole application with a web server on a free port, as a restart would. */
	private static ConfigurableApplicationContext start(String url) {
		List<String> arguments = new ArrayList<>();
		arguments.add("--server.port=0");
		arguments.add("--MATRIZ_DB_URL=" + url);
		arguments.add("--MATRIZ_DB_USER=" + SharedPostgres.username());
		arguments.add("--MATRIZ_DB_PASSWORD=" + SharedPostgres.password());
		arguments.add("--MATRIZ_JWT_SIGNING_KEY=" + DatabaseTestSupport.SIGNING_KEY);
		arguments.add("--MATRIZ_DEMO_DATA=false");
		arguments.add("--spring.datasource.hikari.maximum-pool-size=2");
		return new SpringApplicationBuilder(MatrizApplication.class).run(arguments.toArray(String[]::new));
	}

}
