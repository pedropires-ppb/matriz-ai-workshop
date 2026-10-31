package com.matriz;

import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManagerFactory;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.Location;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * The versioned migrations create the whole data model with the constraints of ADR-007 decision 8,
 * and nothing else changes the schema.
 */
class SchemaMigrationTests extends DatabaseTestSupport {

	private static final String UNIQUE_VIOLATION = "23505";

	private static final String FOREIGN_KEY_VIOLATION = "23503";

	private static final String RESTRICT_VIOLATION = "23001";

	private static final String CHECK_VIOLATION = "23514";

	private static final String NOT_NULL_VIOLATION = "23502";

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private Flyway flyway;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	void theFirstMigrationCreatesTheWholeDataModel() {
		assertThat(this.jdbc.sql("""
				SELECT table_name FROM information_schema.tables
				WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
				""").query(String.class).list())
			.containsExactlyInAnyOrder("flyway_schema_history", "person", "project", "membership", "task",
					"assignment", "comment");
		assertThat(this.jdbc.sql("SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank")
			.query()
			.listOfRows())
			.containsExactly(Map.of("version", "1", "description", "create schema", "success", true));
		// information_schema.sequences omits identity sequences, so pg_class is queried instead.
		assertThat(this.jdbc.sql("""
				SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
				WHERE n.nspname = 'public' AND c.relkind = 'S'
				""").query(String.class).list()).containsExactly("task_creation_order_seq");
	}

	@Test
	void hibernateOnlyValidatesTheSchemaAndFlywayCannotCleanIt() {
		assertThat(this.entityManagerFactory.getProperties()).containsEntry("hibernate.hbm2ddl.auto", "validate");
		assertThat(this.flyway.getConfiguration().isCleanDisabled()).isTrue();
		assertThatThrownBy(this.flyway::clean).isInstanceOf(FlywayException.class);
		assertThat(this.jdbc.sql("SELECT count(*) FROM flyway_schema_history").query(Integer.class).single()).isOne();
	}

	@Test
	void testDatabasesHaveNoDemonstrationData() {
		assertThat(this.flyway.getConfiguration().getLocations()).extracting(Location::getDescriptor)
			.containsExactly("classpath:db/migration");
		assertThat(this.jdbc.sql("SELECT version FROM flyway_schema_history ORDER BY installed_rank")
			.query(String.class)
			.list()).containsExactly("1");
		assertThat(this.jdbc.sql("SELECT count(*) FROM person WHERE login_name IN ('ana', 'rui', 'eva', 'daniel')")
			.query(Integer.class)
			.single()).isZero();
	}

	@Test
	void projectsAndTasksStartWithTheirInitialValues() {
		UUID project = project();
		UUID first = task(project);
		UUID second = task(project);
		assertThat(this.jdbc.sql("SELECT state FROM project WHERE project_id = :id").param("id", project)
			.query(String.class)
			.single()).isEqualTo("ACTIVE");
		assertThat(this.jdbc.sql("SELECT state, priority, deleted_at FROM task WHERE task_id = :id")
			.param("id", first)
			.query()
			.singleRow()).containsEntry("state", "TO_DO").containsEntry("priority", "MEDIUM").containsEntry("deleted_at", null);
		assertThat(creationOrder(second)).isGreaterThan(creationOrder(first));
	}

	@Test
	void textMustNotBeEmpty() {
		UUID project = project();
		UUID task = task(project);
		UUID person = person();
		assertRejected(() -> insertPerson(UUID.randomUUID(), "", "Name"), CHECK_VIOLATION,
				"person_login_name_nonempty_ck");
		assertRejected(() -> insertPerson(UUID.randomUUID(), uniqueLogin(), ""), CHECK_VIOLATION,
				"person_display_name_nonempty_ck");
		assertRejected(() -> this.jdbc.sql("UPDATE person SET password_hash = '' WHERE person_id = :id")
			.param("id", person)
			.update(), CHECK_VIOLATION, "person_password_hash_nonempty_ck");
		assertRejected(() -> this.jdbc.sql("INSERT INTO project (project_id, name) VALUES (:id, '')")
			.param("id", UUID.randomUUID())
			.update(), CHECK_VIOLATION, "project_name_nonempty_ck");
		assertRejected(() -> this.jdbc.sql("INSERT INTO task (task_id, project_id, title) VALUES (:id, :project, '')")
			.param("id", UUID.randomUUID())
			.param("project", project)
			.update(), CHECK_VIOLATION, "task_title_nonempty_ck");
		assertRejected(() -> comment(task, person, ""), CHECK_VIOLATION, "comment_text_nonempty_ck");
	}

	@Test
	void requiredValuesMustBePresent() {
		UUID project = project();
		assertRejectedAsNull(() -> this.jdbc.sql("INSERT INTO task (task_id, project_id, title) VALUES (:id, :project, NULL)")
			.param("id", UUID.randomUUID())
			.param("project", project)
			.update(), "title");
		assertRejectedAsNull(() -> this.jdbc.sql("""
				INSERT INTO comment (comment_id, task_id, author_person_id, text, created_at)
				VALUES (:id, :task, :author, 'Text', NULL)
				""")
			.param("id", UUID.randomUUID())
			.param("task", task(project))
			.param("author", person())
			.update(), "created_at");
		assertRejectedAsNull(() -> membership(project, person(), null), "role");
	}

	@Test
	void enumerationsAcceptOnlyTheirCodes() {
		UUID project = project();
		UUID task = task(project);
		assertRejected(() -> this.jdbc.sql("UPDATE project SET state = 'ARCHIVED' WHERE project_id = :id")
			.param("id", project)
			.update(), CHECK_VIOLATION, "project_state_ck");
		assertRejected(() -> membership(project, person(), "MANAGER"), CHECK_VIOLATION, "membership_role_ck");
		assertRejected(() -> this.jdbc.sql("UPDATE task SET state = 'BLOCKED' WHERE task_id = :id")
			.param("id", task)
			.update(), CHECK_VIOLATION, "task_state_ck");
		assertRejected(() -> this.jdbc.sql("UPDATE task SET priority = 'URGENT' WHERE task_id = :id")
			.param("id", task)
			.update(), CHECK_VIOLATION, "task_priority_ck");
		this.jdbc.sql("UPDATE project SET state = 'COMPLETED' WHERE project_id = :id").param("id", project).update();
		this.jdbc.sql("UPDATE task SET state = 'DONE', priority = 'HIGH' WHERE task_id = :id").param("id", task).update();
	}

	@Test
	void loginNamesAreUniqueAndCaseSensitive() {
		String login = uniqueLogin();
		insertPerson(UUID.randomUUID(), login, "First");
		assertRejected(() -> insertPerson(UUID.randomUUID(), login, "Second"), UNIQUE_VIOLATION,
				"person_login_name_uk");
		insertPerson(UUID.randomUUID(), login.toUpperCase(), "Third");
	}

	@Test
	void aProjectHasAtMostOneProjectOwner() {
		UUID project = project();
		membership(project, person(), "PROJECT_OWNER");
		membership(project, person(), "PROJECT_PARTICIPANT");
		assertRejected(() -> membership(project, person(), "PROJECT_OWNER"), UNIQUE_VIOLATION,
				"membership_one_owner_ux");
		membership(project(), person(), "PROJECT_OWNER");
	}

	@Test
	void anAssignmentStaysWithinItsTasksProjectAndItsMembers() {
		UUID project = project();
		UUID otherProject = project();
		UUID task = task(project);
		UUID member = person();
		membership(project, member, "PROJECT_PARTICIPANT");
		membership(otherProject, member, "PROJECT_PARTICIPANT");
		assignment(task, member, project);
		assertRejected(() -> assignment(task, person(), project), FOREIGN_KEY_VIOLATION, "assignment_membership_fk");
		assertRejected(() -> assignment(task(project), member, otherProject), FOREIGN_KEY_VIOLATION,
				"assignment_task_fk");
	}

	@Test
	void aMembershipThatAnAssignmentReferencesCannotBeDeleted() {
		UUID project = project();
		UUID assignee = person();
		UUID other = person();
		membership(project, assignee, "PROJECT_PARTICIPANT");
		membership(project, other, "PROJECT_PARTICIPANT");
		assignment(task(project), assignee, project);
		assertRejected(() -> deleteMembership(project, assignee), RESTRICT_VIOLATION, "assignment_membership_fk");
		assertThat(deleteMembership(project, other)).isOne();
	}

	@Test
	void everyReferenceMustExist() {
		UUID project = project();
		UUID person = person();
		UUID task = task(project);
		assertRejected(() -> membership(UUID.randomUUID(), person, "PROJECT_OWNER"), FOREIGN_KEY_VIOLATION,
				"membership_project_fk");
		assertRejected(() -> membership(project, UUID.randomUUID(), "PROJECT_OWNER"), FOREIGN_KEY_VIOLATION,
				"membership_person_fk");
		assertRejected(() -> task(UUID.randomUUID()), FOREIGN_KEY_VIOLATION, "task_project_fk");
		assertRejected(() -> comment(UUID.randomUUID(), person, "Text"), FOREIGN_KEY_VIOLATION, "comment_task_fk");
		assertRejected(() -> comment(task, UUID.randomUUID(), "Text"), FOREIGN_KEY_VIOLATION, "comment_author_fk");
	}

	@Test
	void nothingCascades() {
		UUID projectWithMember = project();
		UUID member = person();
		membership(projectWithMember, member, "PROJECT_OWNER");
		assertRejected(() -> deleteProject(projectWithMember), FOREIGN_KEY_VIOLATION, "membership_project_fk");
		assertRejected(() -> deletePerson(member), FOREIGN_KEY_VIOLATION, "membership_person_fk");

		UUID projectWithTask = project();
		UUID task = task(projectWithTask);
		assertRejected(() -> deleteProject(projectWithTask), FOREIGN_KEY_VIOLATION, "task_project_fk");

		UUID author = person();
		comment(task, author, "Text");
		assertRejected(() -> deletePerson(author), FOREIGN_KEY_VIOLATION, "comment_author_fk");
		assertRejected(() -> this.jdbc.sql("DELETE FROM task WHERE task_id = :id").param("id", task).update(),
				FOREIGN_KEY_VIOLATION, "comment_task_fk");
	}

	private int deleteProject(UUID project) {
		return this.jdbc.sql("DELETE FROM project WHERE project_id = :id").param("id", project).update();
	}

	private int deletePerson(UUID person) {
		return this.jdbc.sql("DELETE FROM person WHERE person_id = :id").param("id", person).update();
	}

	private void assertRejected(ThrowingCallable statement, String sqlState, String constraint) {
		assertThat(serverError(statement, sqlState).getConstraint()).isEqualTo(constraint);
	}

	private void assertRejectedAsNull(ThrowingCallable statement, String column) {
		assertThat(serverError(statement, NOT_NULL_VIOLATION).getColumn()).isEqualTo(column);
	}

	private ServerErrorMessage serverError(ThrowingCallable statement, String sqlState) {
		Throwable thrown = catchThrowable(statement);
		assertThat(thrown).isInstanceOf(DataAccessException.class);
		PSQLException cause = postgresCause(thrown);
		assertThat(cause.getSQLState()).isEqualTo(sqlState);
		ServerErrorMessage message = cause.getServerErrorMessage();
		assertThat(message).isNotNull();
		return message;
	}

	private static PSQLException postgresCause(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof PSQLException postgres) {
				return postgres;
			}
		}
		throw new AssertionError("No PostgreSQL error in " + ex, ex);
	}

	private UUID person() {
		UUID id = UUID.randomUUID();
		insertPerson(id, uniqueLogin(), "Test person");
		return id;
	}

	private void insertPerson(UUID id, String loginName, String displayName) {
		this.jdbc.sql("""
				INSERT INTO person (person_id, login_name, display_name, password_hash)
				VALUES (:id, :loginName, :displayName, 'not-a-real-hash')
				""").param("id", id).param("loginName", loginName).param("displayName", displayName).update();
	}

	private static String uniqueLogin() {
		return "person-" + UUID.randomUUID();
	}

	private UUID project() {
		UUID id = UUID.randomUUID();
		this.jdbc.sql("INSERT INTO project (project_id, name) VALUES (:id, 'Test project')").param("id", id).update();
		return id;
	}

	private void membership(UUID project, UUID person, String role) {
		this.jdbc.sql("INSERT INTO membership (project_id, person_id, role) VALUES (:project, :person, :role)")
			.param("project", project)
			.param("person", person)
			.param("role", role)
			.update();
	}

	private int deleteMembership(UUID project, UUID person) {
		return this.jdbc.sql("DELETE FROM membership WHERE project_id = :project AND person_id = :person")
			.param("project", project)
			.param("person", person)
			.update();
	}

	private UUID task(UUID project) {
		UUID id = UUID.randomUUID();
		this.jdbc.sql("INSERT INTO task (task_id, project_id, title) VALUES (:id, :project, 'Test task')")
			.param("id", id)
			.param("project", project)
			.update();
		return id;
	}

	private long creationOrder(UUID task) {
		return this.jdbc.sql("SELECT creation_order FROM task WHERE task_id = :id")
			.param("id", task)
			.query(Long.class)
			.single();
	}

	private void assignment(UUID task, UUID person, UUID project) {
		this.jdbc.sql("INSERT INTO assignment (task_id, person_id, project_id) VALUES (:task, :person, :project)")
			.param("task", task)
			.param("person", person)
			.param("project", project)
			.update();
	}

	private void comment(UUID task, UUID author, String text) {
		this.jdbc.sql("""
				INSERT INTO comment (comment_id, task_id, author_person_id, text, created_at)
				VALUES (:id, :task, :author, :text, now())
				""")
			.param("id", UUID.randomUUID())
			.param("task", task)
			.param("author", author)
			.param("text", text)
			.update();
	}

}
