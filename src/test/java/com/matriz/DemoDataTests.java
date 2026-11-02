package com.matriz;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The demonstration data, applied by a Flyway migration (ADR-007 decision 10, amended on
 * 2026-10-30). This is the only test class that turns it on: each test starts the whole
 * application, without a web server, against its own empty database.
 */
@ExtendWith(OutputCaptureExtension.class)
class DemoDataTests {

	private static final UUID ANA = UUID.fromString("10000000-0000-4000-8000-000000000001");

	private static final UUID RUI = UUID.fromString("10000000-0000-4000-8000-000000000002");

	private static final UUID EVA = UUID.fromString("10000000-0000-4000-8000-000000000003");

	private static final UUID DANIEL = UUID.fromString("10000000-0000-4000-8000-000000000004");

	private static final UUID WEBSITE_REDESIGN = UUID.fromString("20000000-0000-4000-8000-000000000001");

	private static final UUID SECOND_COMMENT = UUID.fromString("40000000-0000-4000-8000-000000000002");

	private static final Map<String, String> PASSWORDS = Map.of("MATRIZ_DEMO_PASSWORD_ANA", "fictitious-ana-1",
			"MATRIZ_DEMO_PASSWORD_RUI", "fictitious-rui-1", "MATRIZ_DEMO_PASSWORD_EVA", "fictitious-eva-1",
			"MATRIZ_DEMO_PASSWORD_DANIEL", "fictitious-daniel-1");

	private static final Map<String, String> CHANGED_PASSWORDS = Map.of("MATRIZ_DEMO_PASSWORD_ANA", "fictitious-ana-2",
			"MATRIZ_DEMO_PASSWORD_RUI", "fictitious-rui-2", "MATRIZ_DEMO_PASSWORD_EVA", "fictitious-eva-2",
			"MATRIZ_DEMO_PASSWORD_DANIEL", "fictitious-daniel-2");

	private static final Map<String, String> DEMO_DATA_OFF = Map.of("MATRIZ_DEMO_DATA", "false");

	/** A database created by I1, before the schema migrations that followed the demonstration data. */
	private static final Map<String, String> I1_DATABASE = Map.of("MATRIZ_DEMO_DATA", "false",
			"spring.flyway.target", "1");

	private static final String[] WITH_DEMONSTRATION_DATA = { "1", "1.1", "2", "3" };

	private static final String[] WITHOUT_DEMONSTRATION_DATA = { "1", "2", "3" };

	private static final Map<String, Integer> DEMONSTRATION_COUNTS = counts(4, 4, 8, 9, 3, 2);

	private static final Map<String, Integer> NO_ROWS = counts(0, 0, 0, 0, 0, 0);

	private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	@Test
	void theDemonstrationDataIsAppliedOnceAndARestartChangesNothing(CapturedOutput output) {
		String url = SharedPostgres.newDatabase();
		JdbcClient jdbc = jdbc(url);

		start(url, PASSWORDS);
		assertThat(history(jdbc)).containsExactly(WITH_DEMONSTRATION_DATA);
		assertThat(counts(jdbc)).isEqualTo(DEMONSTRATION_COUNTS);
		assertTheDemonstrationContents(jdbc);
		assertThat(output).contains("Demonstration data is on").contains("now at version v3");
		String anaHash = passwordHash(jdbc, ANA);

		UUID handAdded = UUID.randomUUID();
		jdbc.sql("""
				INSERT INTO person (person_id, login_name, display_name, password_hash)
				VALUES (:id, 'hand-added', 'Added by hand', 'not-a-real-hash')
				""").param("id", handAdded).update();
		jdbc.sql("UPDATE project SET name = 'Renamed by hand' WHERE project_id = :id")
			.param("id", WEBSITE_REDESIGN)
			.update();
		jdbc.sql("DELETE FROM comment WHERE comment_id = :id").param("id", SECOND_COMMENT).update();

		int firstStart = output.getAll().length();
		start(url, CHANGED_PASSWORDS);
		assertThat(output.getAll().substring(firstStart)).contains("is up to date. No migration necessary.");
		assertThat(history(jdbc)).containsExactly(WITH_DEMONSTRATION_DATA);
		assertThat(counts(jdbc)).isEqualTo(counts(5, 4, 8, 9, 3, 1));
		assertThat(jdbc.sql("SELECT display_name FROM person WHERE person_id = :id")
			.param("id", handAdded)
			.query(String.class)
			.single()).isEqualTo("Added by hand");
		assertThat(jdbc.sql("SELECT name FROM project WHERE project_id = :id")
			.param("id", WEBSITE_REDESIGN)
			.query(String.class)
			.single()).isEqualTo("Renamed by hand");
		assertThat(jdbc.sql("SELECT count(*) FROM comment WHERE comment_id = :id")
			.param("id", SECOND_COMMENT)
			.query(Integer.class)
			.single()).isZero();
		assertThat(passwordHash(jdbc, ANA)).isEqualTo(anaHash);
		assertNoPasswordOrHash(output);
	}

	@Test
	void aBlankDemonstrationPasswordStopsStartUpBeforeTheDatabaseIsUsed(CapturedOutput output) {
		String url = SharedPostgres.newDatabase();
		Map<String, String> variables = new HashMap<>(PASSWORDS);
		variables.put("MATRIZ_DEMO_PASSWORD_EVA", " ");

		assertThatThrownBy(() -> start(url, variables)).isInstanceOf(EnvironmentVariablesException.class)
			.hasMessage("These environment variables are missing or blank: MATRIZ_DEMO_PASSWORD_EVA.");
		assertThat(output).contains("Matriz cannot start. These environment variables are missing or blank: "
				+ "MATRIZ_DEMO_PASSWORD_EVA.");
		assertThat(tables(jdbc(url))).isEmpty();
		assertNoPasswordOrHash(output);
	}

	@Test
	void aDemonstrationPasswordThatBreaksThePasswordRulesStopsStartUpBeforeTheDatabaseIsUsed(CapturedOutput output) {
		String url = SharedPostgres.newDatabase();
		Map<String, String> variables = new HashMap<>(PASSWORDS);
		variables.put("MATRIZ_DEMO_PASSWORD_EVA", "fictitious-eva");

		assertThatThrownBy(() -> start(url, variables)).isInstanceOf(EnvironmentVariablesException.class)
			.hasMessage("These environment variables must have at least 15 characters and at most 72 bytes in UTF-8: "
					+ "MATRIZ_DEMO_PASSWORD_EVA.");
		assertThat(output).contains("Matriz cannot start. These environment variables must have at least 15 "
				+ "characters").doesNotContain("fictitious-eva");
		assertThat(tables(jdbc(url))).isEmpty();
		assertNoPasswordOrHash(output);
	}

	@Test
	void aDatabaseStartedWithoutTheDemonstrationDataCanGetItWhileAtItsSchemaVersion(CapturedOutput output) {
		String url = SharedPostgres.newDatabase();
		JdbcClient jdbc = jdbc(url);
		start(url, I1_DATABASE);
		assertThat(history(jdbc)).containsExactly("1");
		assertThat(counts(jdbc)).isEqualTo(NO_ROWS);

		start(url, PASSWORDS);
		assertThat(history(jdbc)).containsExactly(WITH_DEMONSTRATION_DATA);
		assertThat(counts(jdbc)).isEqualTo(DEMONSTRATION_COUNTS);
		assertNoPasswordOrHash(output);
	}

	@Test
	void rowsThatConflictWithTheDemonstrationDataStopStartUpAndNoneOfItIsAdded(CapturedOutput output) {
		String url = SharedPostgres.newDatabase();
		JdbcClient jdbc = jdbc(url);
		start(url, I1_DATABASE);
		jdbc.sql("""
				INSERT INTO person (person_id, login_name, display_name, password_hash)
				VALUES (:id, 'eva', 'Another person', 'not-a-real-hash')
				""").param("id", UUID.randomUUID()).update();

		assertThatThrownBy(() -> start(url, PASSWORDS)).rootCause()
			.isInstanceOf(DemoDataException.class)
			.hasNoCause()
			.hasMessage("The demonstration data (migration 1.1) cannot be added to this database: ERROR: duplicate key "
					+ "value violates unique constraint \"person_login_name_uk\" (SQL state 23505). None of it was added.");
		assertThat(output).contains("Matriz cannot start. The demonstration data (migration 1.1) cannot be added")
			.contains("Change or remove the rows that conflict with the demonstration data, set MATRIZ_DEMO_DATA=false, "
					+ "or reset the local database as described in README.md (this deletes all its data).");
		assertThat(history(jdbc)).containsExactly("1");
		assertThat(counts(jdbc)).isEqualTo(counts(1, 0, 0, 0, 0, 0));
		assertNoPasswordOrHash(output);
	}

	@Test
	void turningTheDemonstrationDataOffForADatabaseThatHasItStopsStartUp(CapturedOutput output) {
		String url = SharedPostgres.newDatabase();
		JdbcClient jdbc = jdbc(url);
		start(url, PASSWORDS);

		assertThatThrownBy(() -> start(url, DEMO_DATA_OFF)).rootCause()
			.isInstanceOf(DemoDataException.class)
			.hasMessage("The demonstration data (migration 1.1) was added to this database, but MATRIZ_DEMO_DATA is not "
					+ "true. The setting is fixed for each database.");
		assertThat(output).contains("Set MATRIZ_DEMO_DATA=true and the demonstration passwords");
		assertThat(history(jdbc)).containsExactly(WITH_DEMONSTRATION_DATA);
		assertThat(counts(jdbc)).isEqualTo(DEMONSTRATION_COUNTS);
	}

	@Test
	void theDemonstrationDataCannotBeAddedOnceTheSchemaIsPastItsVersion(CapturedOutput output) {
		String url = SharedPostgres.newDatabase();
		JdbcClient jdbc = jdbc(url);
		start(url, DEMO_DATA_OFF);
		assertThat(history(jdbc)).containsExactly(WITHOUT_DEMONSTRATION_DATA);

		assertThatThrownBy(() -> start(url, PASSWORDS)).rootCause()
			.isInstanceOf(DemoDataException.class)
			.hasMessage("The demonstration data (migration 1.1) cannot be added to this database, because its schema "
					+ "is already at version 3. The setting is fixed for each database.");
		assertThat(output).contains("Set MATRIZ_DEMO_DATA=false, or reset the local database as described in README.md");
		assertThat(history(jdbc)).containsExactly(WITHOUT_DEMONSTRATION_DATA);
		assertThat(counts(jdbc)).isEqualTo(NO_ROWS);
		assertNoPasswordOrHash(output);
	}

	private void assertTheDemonstrationContents(JdbcClient jdbc) {
		assertThat(jdbc.sql("SELECT person_id, login_name, display_name FROM person ORDER BY person_id")
			.query((rs, row) -> tuple(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3)))
			.list()).containsExactly(tuple(ANA, "ana", "Ana Silva"), tuple(RUI, "rui", "Rui Martins"),
					tuple(EVA, "eva", "Eva Santos"), tuple(DANIEL, "daniel", "Daniel Rocha"));
		Map<UUID, String> passwords = Map.of(ANA, "fictitious-ana-1", RUI, "fictitious-rui-1", EVA, "fictitious-eva-1",
				DANIEL, "fictitious-daniel-1");
		passwords.forEach((person, password) -> {
			String hash = passwordHash(jdbc, person);
			assertThat(hash).startsWith("{bcrypt}").doesNotContain(password);
			assertThat(this.passwordEncoder.matches(password, hash)).isTrue();
		});

		assertThat(jdbc.sql("SELECT name, state FROM project ORDER BY project_id")
			.query((rs, row) -> tuple(rs.getString(1), rs.getString(2)))
			.list()).containsExactly(tuple("Website Redesign", "ACTIVE"), tuple("Mobile App", "ACTIVE"),
					tuple("Website Audit", "ACTIVE"), tuple("Project Gamma", "COMPLETED"));
		assertThat(projectsOf(jdbc, ANA)).containsExactly("Mobile App", "Project Gamma", "Website Redesign");
		assertThat(projectsOf(jdbc, RUI)).containsExactly("Mobile App", "Project Gamma", "Website Redesign");
		assertThat(projectsOf(jdbc, EVA)).containsExactly("Website Audit", "Website Redesign");
		assertThat(projectsOf(jdbc, DANIEL)).isEmpty();
		assertThat(jdbc.sql("""
				SELECT p.name, m.login_name FROM membership ms
				JOIN project p ON p.project_id = ms.project_id
				JOIN person m ON m.person_id = ms.person_id
				WHERE ms.role = 'PROJECT_OWNER' ORDER BY p.name
				""").query((rs, row) -> tuple(rs.getString(1), rs.getString(2))).list())
			.containsExactly(tuple("Mobile App", "rui"), tuple("Project Gamma", "ana"),
					tuple("Website Audit", "eva"), tuple("Website Redesign", "ana"));

		assertThat(jdbc.sql("""
				SELECT p.name, t.title, t.state, t.priority, t.deleted_at FROM task t
				JOIN project p ON p.project_id = t.project_id ORDER BY t.creation_order
				""")
			.query((rs, row) -> tuple(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
					rs.getObject(5)))
			.list()).containsExactly(tuple("Website Redesign", "T1", "TO_DO", "HIGH", null),
					tuple("Website Redesign", "T2", "IN_PROGRESS", "HIGH", null),
					tuple("Website Redesign", "T3", "DONE", "LOW", null),
					tuple("Website Redesign", "T4", "TO_DO", "MEDIUM", null),
					tuple("Mobile App", "Prepare proposal", "TO_DO", "MEDIUM", null),
					tuple("Mobile App", "Review proposal", "IN_PROGRESS", "HIGH", null),
					tuple("Project Gamma", "T1", "DONE", "MEDIUM", null),
					tuple("Project Gamma", "T2", "DONE", "MEDIUM", null),
					tuple("Project Gamma", "T3", "DONE", "MEDIUM", null));
		assertThat(jdbc.sql("""
				SELECT p.name, t.title, m.login_name FROM assignment a
				JOIN task t ON t.task_id = a.task_id
				JOIN project p ON p.project_id = a.project_id
				JOIN person m ON m.person_id = a.person_id
				ORDER BY p.name, t.title, m.login_name
				""").query((rs, row) -> tuple(rs.getString(1), rs.getString(2), rs.getString(3))).list())
			.containsExactly(tuple("Mobile App", "Review proposal", "ana"), tuple("Website Redesign", "T1", "ana"),
					tuple("Website Redesign", "T1", "rui"));
		assertThat(jdbc.sql("""
				SELECT t.title, c.text, m.login_name, c.created_at FROM comment c
				JOIN task t ON t.task_id = c.task_id
				JOIN person m ON m.person_id = c.author_person_id
				ORDER BY c.created_at, c.comment_id
				""")
			.query((rs, row) -> tuple(rs.getString(1), rs.getString(2), rs.getString(3),
					rs.getObject(4, OffsetDateTime.class).toInstant()))
			.list()).containsExactly(
					tuple("Review proposal", "First", "ana", OffsetDateTime.parse("2026-10-29T09:30:00Z").toInstant()),
					tuple("Review proposal", "Second", "rui", OffsetDateTime.parse("2026-10-29T10:05:00Z").toInstant()));
	}

	/** Neither the passwords nor the hashes that the migration receives, nor the key, reach the output. */
	private static void assertNoPasswordOrHash(CapturedOutput output) {
		assertThat(output).doesNotContain("{bcrypt}").doesNotContain("$2a$").doesNotContain(DatabaseTestSupport.SIGNING_KEY);
		PASSWORDS.values().forEach((password) -> assertThat(output).doesNotContain(password));
		CHANGED_PASSWORDS.values().forEach((password) -> assertThat(output).doesNotContain(password));
	}

	private static List<String> history(JdbcClient jdbc) {
		return jdbc.sql("SELECT version FROM flyway_schema_history ORDER BY installed_rank").query(String.class).list();
	}

	private static List<String> tables(JdbcClient jdbc) {
		return jdbc.sql("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")
			.query(String.class)
			.list();
	}

	private static List<String> projectsOf(JdbcClient jdbc, UUID person) {
		return jdbc.sql("""
				SELECT p.name FROM membership ms JOIN project p ON p.project_id = ms.project_id
				WHERE ms.person_id = :person ORDER BY p.name
				""").param("person", person).query(String.class).list();
	}

	private static String passwordHash(JdbcClient jdbc, UUID person) {
		return jdbc.sql("SELECT password_hash FROM person WHERE person_id = :id")
			.param("id", person)
			.query(String.class)
			.single();
	}

	private static Map<String, Integer> counts(JdbcClient jdbc) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (String table : DEMONSTRATION_COUNTS.keySet()) {
			counts.put(table, jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single());
		}
		return counts;
	}

	private static Map<String, Integer> counts(int people, int projects, int memberships, int tasks, int assignments,
			int comments) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		counts.put("person", people);
		counts.put("project", projects);
		counts.put("membership", memberships);
		counts.put("task", tasks);
		counts.put("assignment", assignments);
		counts.put("comment", comments);
		return counts;
	}

	private static JdbcClient jdbc(String url) {
		return JdbcClient.create(new DriverManagerDataSource(url, SharedPostgres.username(), SharedPostgres.password()));
	}

	/** Starts and stops the whole application, as a restart would. */
	private static void start(String url, Map<String, String> variables) {
		Map<String, String> arguments = new LinkedHashMap<>();
		arguments.put("MATRIZ_DB_URL", url);
		arguments.put("MATRIZ_DB_USER", SharedPostgres.username());
		arguments.put("MATRIZ_DB_PASSWORD", SharedPostgres.password());
		arguments.put("MATRIZ_JWT_SIGNING_KEY", DatabaseTestSupport.SIGNING_KEY);
		arguments.put("MATRIZ_DEMO_DATA", "true");
		arguments.put("spring.datasource.hikari.maximum-pool-size", "2");
		arguments.putAll(variables);
		List<String> args = new ArrayList<>();
		arguments.forEach((name, value) -> args.add("--" + name + "=" + value));
		new SpringApplicationBuilder(MatrizApplication.class).web(WebApplicationType.NONE)
			.run(args.toArray(String[]::new))
			.close();
	}

}
