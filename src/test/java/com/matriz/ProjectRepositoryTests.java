package com.matriz;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.matriz.identity.PersonId;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectId;
import com.matriz.projects.ProjectState;
import com.matriz.projects.domain.Project;
import com.matriz.projects.domain.ProjectDescription;
import com.matriz.projects.domain.ProjectName;
import com.matriz.projects.domain.ProjectRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How a project is stored (ADR-008, open point "Mapping conventions"): a project stored and then
 * read in a new persistence context keeps every field, each in its column of the schema
 * (docs/database.md); a deleted project is invisible to every read.
 */
class ProjectRepositoryTests extends DatabaseTestSupport {

	@Autowired
	private ProjectRepository projects;

	@Autowired
	private TransactionTemplate transactions;

	@Autowired
	private JdbcClient jdbc;

	@Test
	void aStoredProjectIsReadInANewPersistenceContextWithEveryField() {
		PersonId owner = person("Ana Silva");
		Project stored = store(Project.create(ProjectId.generate(), new ProjectName("Website"),
				ProjectDescription.ofNullable("About"), owner));

		Project read = this.transactions.execute((transaction) -> this.projects.findById(stored.id()).orElseThrow());

		assertThat(read).isNotSameAs(stored);
		assertThat(read.id()).isEqualTo(stored.id());
		assertThat(read.name().value()).isEqualTo("Website");
		assertThat(read.description().orElseThrow().value()).isEqualTo("About");
		assertThat(read.state()).isEqualTo(ProjectState.ACTIVE);
		assertThat(read.roleOf(owner)).contains(MemberRole.PROJECT_OWNER);
	}

	@Test
	void eachFieldIsStoredInItsColumn() {
		PersonId owner = person("Ana Silva");
		Project stored = store(Project.create(ProjectId.generate(), new ProjectName("Website"),
				ProjectDescription.ofNullable("About"), owner));

		assertThat(this.jdbc.sql("SELECT name, description, state, deleted_at FROM project WHERE project_id = ?")
			.param(stored.id().value())
			.query((row, number) -> List.of(row.getString(1), row.getString(2), row.getString(3),
					String.valueOf(row.getObject(4))))
			.single()).containsExactly("Website", "About", "ACTIVE", "null");
		assertThat(this.jdbc.sql("SELECT person_id, role FROM membership WHERE project_id = ?")
			.param(stored.id().value())
			.query((row, number) -> List.of(row.getObject(1, UUID.class).toString(), row.getString(2)))
			.single()).containsExactly(owner.value().toString(), "PROJECT_OWNER");
	}

	@Test
	void noDescriptionIsReadBackAsNone() {
		Project stored = store(Project.create(ProjectId.generate(), new ProjectName("Plain"), null, person("Rui")));

		Project read = this.transactions.execute((transaction) -> this.projects.findById(stored.id()).orElseThrow());

		assertThat(read.description()).isEmpty();
	}

	@Test
	void anEditIsStoredAndNoDescriptionIsStoredAsNull() {
		PersonId owner = person("Ana Silva");
		Project stored = store(Project.create(ProjectId.generate(), new ProjectName("Old"),
				ProjectDescription.ofNullable("Text"), owner));

		this.transactions.executeWithoutResult((transaction) -> this.projects.findByIdForUpdate(stored.id())
			.orElseThrow()
			.edit(owner, new com.matriz.projects.domain.ProjectEdit(new ProjectName("New"), true, null)));

		assertThat(this.jdbc.sql("SELECT name FROM project WHERE project_id = ?")
			.param(stored.id().value())
			.query(String.class)
			.single()).isEqualTo("New");
		assertThat(this.jdbc.sql("SELECT count(*) FROM project WHERE project_id = ? AND description IS NULL")
			.param(stored.id().value())
			.query(Integer.class)
			.single()).isEqualTo(1);
	}

	@Test
	void aDeletedProjectIsNotFoundAndNotListedButItsRowsStay() {
		PersonId owner = person("Ana Silva");
		Project stored = store(Project.create(ProjectId.generate(), new ProjectName("Doomed"), null, owner));

		this.transactions.executeWithoutResult((transaction) -> this.projects.findByIdForUpdate(stored.id())
			.orElseThrow()
			.delete(owner, java.time.Instant.now()));

		assertThat(this.projects.findById(stored.id())).isEmpty();
		Optional<Project> locked = this.transactions
			.execute((transaction) -> this.projects.findByIdForUpdate(stored.id()));
		assertThat(locked).isEmpty();
		assertThat(this.projects.findAccessible(owner, null, 1, 20).items()).isEmpty();
		assertThat(this.projects.findAccessible(owner, null, 1, 20).totalItems()).isZero();
		assertThat(this.jdbc.sql("SELECT count(*) FROM project WHERE project_id = ? AND deleted_at IS NOT NULL")
			.param(stored.id().value())
			.query(Integer.class)
			.single()).isEqualTo(1);
	}

	private Project store(Project project) {
		return this.transactions.execute((transaction) -> this.projects.save(project));
	}

	private PersonId person(String displayName) {
		return new PersonId(TestAccess.addPerson(this.jdbc, displayName).id());
	}

}
