package com.matriz;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A signed-out token stays refused after Matriz restarts, because sign-outs are kept in the
 * database; replacing the signing key and restarting refuses every token issued before (ADR-005,
 * open points "How sign-out is enforced" and "Signing"). Matriz runs with a web server here, and
 * is called over HTTP, as a client outside the User interface would call it.
 */
@ExtendWith(OutputCaptureExtension.class)
class SignOutAfterRestartTests {

	private static final String INVALID_TOKEN_CHALLENGE = TestAccess.INVALID_TOKEN_CHALLENGE;

	@Test
	void aSignedOutTokenStaysRefusedAfterARestartAndANewKeyRefusesEveryToken(CapturedOutput output)
			throws Exception {
		String url = SharedPostgres.newDatabase();
		String signedOut;
		String kept;
		TestAccess.TestPerson rui;
		try (HttpClient http = HttpClient.newHttpClient()) {
			try (ConfigurableApplicationContext matriz = start(url, DatabaseTestSupport.SIGNING_KEY)) {
				rui = TestAccess.addPerson(jdbc(url), "Rui Martins");
				signedOut = signIn(http, matriz, rui);
				kept = signIn(http, matriz, rui);
				assertThat(send(http, matriz, "DELETE", "/api/session", signedOut).statusCode()).isEqualTo(204);
				assertThat(send(http, matriz, "GET", "/api/me", signedOut).statusCode()).isEqualTo(401);
			}

			try (ConfigurableApplicationContext matriz = start(url, DatabaseTestSupport.SIGNING_KEY)) {
				assertRefused(send(http, matriz, "GET", "/api/me", signedOut));
				HttpResponse<String> me = send(http, matriz, "GET", "/api/me", kept);
				assertThat(me.statusCode()).isEqualTo(200);
				assertThat(JsonMapper.shared().readTree(me.body()).path("personId").stringValue())
					.isEqualTo(rui.id().toString());
			}

			try (ConfigurableApplicationContext matriz = start(url, DatabaseTestSupport.randomKey(32))) {
				assertRefused(send(http, matriz, "GET", "/api/me", kept));
				assertThat(signIn(http, matriz, rui)).isNotBlank();
			}
		}

		assertThat(output.getAll()).doesNotContain(rui.password())
			.doesNotContain(signature(signedOut))
			.doesNotContain(signature(kept))
			.doesNotContain(DatabaseTestSupport.SIGNING_KEY);
	}

	private static void assertRefused(HttpResponse<String> response) {
		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.headers().allValues(HttpHeaders.WWW_AUTHENTICATE)).containsExactly(INVALID_TOKEN_CHALLENGE);
		assertThat(response.headers().firstValue(HttpHeaders.CONTENT_TYPE)).hasValue(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		assertThat(JsonMapper.shared().readTree(response.body()).path("detail").stringValue())
			.isEqualTo(TestAccess.INVALID_TOKEN);
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

	private static HttpResponse<String> send(HttpClient http, ConfigurableApplicationContext matriz, String method,
			String path, String token) throws IOException, InterruptedException {
		return http.send(HttpRequest.newBuilder(uri(matriz, path))
			.header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(token))
			.method(method, HttpRequest.BodyPublishers.noBody())
			.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static URI uri(ConfigurableApplicationContext matriz, String path) {
		return URI.create("http://localhost:" + matriz.getEnvironment().getProperty("local.server.port") + path);
	}

	private static String signature(String token) {
		return token.substring(token.lastIndexOf('.') + 1);
	}

	private static JdbcClient jdbc(String url) {
		return JdbcClient.create(new DriverManagerDataSource(url, SharedPostgres.username(), SharedPostgres.password()));
	}

	/** Starts the whole application with a web server on a free port, as a restart would. */
	private static ConfigurableApplicationContext start(String url, String signingKey) {
		List<String> arguments = new ArrayList<>();
		arguments.add("--server.port=0");
		arguments.add("--MATRIZ_DB_URL=" + url);
		arguments.add("--MATRIZ_DB_USER=" + SharedPostgres.username());
		arguments.add("--MATRIZ_DB_PASSWORD=" + SharedPostgres.password());
		arguments.add("--MATRIZ_JWT_SIGNING_KEY=" + signingKey);
		arguments.add("--MATRIZ_DEMO_DATA=false");
		arguments.add("--spring.datasource.hikari.maximum-pool-size=2");
		return new SpringApplicationBuilder(MatrizApplication.class).run(arguments.toArray(String[]::new));
	}

}
