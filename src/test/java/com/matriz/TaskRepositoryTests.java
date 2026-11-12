package com.matriz;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.matriz.identity.PersonId;
import com.matriz.projects.ProjectId;
import com.matriz.projects.domain.Project;
import com.matriz.projects.domain.ProjectName;
import com.matriz.projects.domain.ProjectRepository;
import com.matriz.tasks.TaskId;
import com.matriz.tasks.TaskPriority;
import com.matriz.tasks.TaskState;
import com.matriz.tasks.domain.Task;
import com.matriz.tasks.domain.TaskDescription;
import com.matriz.tasks.domain.TaskEdit;
import com.matriz.tasks.domain.TaskRepository;
import com.matriz.tasks.domain.TaskRepository.TaskPage;
import com.matriz.tasks.domain.TaskTitle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How a task is stored (ADR-008, open points "Mapping conventions" and "creation_order"): a task
 * stored and then read in a new persistence context keeps every field, each in its column of the
 * schema (docs/database.md); the list is in creation order, filtered and paged; a deleted task is
 * invisible to every read and loses its Assignments.
 */
class TaskRepositoryTests extends DatabaseTestSupport {

	@Autowired
	private TaskRepository tasks;

	@Autowired
	private ProjectRepository projects;

	@Autowired
	private TransactionTemplate transactions;

	@Autowired
	private JdbcClient jdbc;

	private PersonId owner;

	private ProjectId project;

	@BeforeEach
	void addAProject() {
		this.owner = new PersonId(TestAccess.addPerson(this.jdbc, "Ana Silva").id());
		this.project = this.transactions
			.execute((transaction) -> this.projects
				.save(Project.create(ProjectId.generate(), new ProjectName("Website"), null, this.owner)))
			.id();
	}

	@Test
	void aStoredTaskIsReadInANewPersistenceContextWithEveryField() {
		Task stored = store("Write tests", "Text", TaskPriority.HIGH);

		Task read = this.transactions.execute((transaction) -> this.tasks.findById(this.project, stored.id()).orElseThrow());

		assertThat(read).isNotSameAs(stored);
		assertThat(read.id()).isEqualTo(stored.id());
		assertThat(read.projectId()).isEqualTo(this.project);
		assertThat(read.title().value()).isEqualTo("Write tests");
		assertThat(read.description().orElseThrow().value()).isEqualTo("Text");
		assertThat(read.state()).isEqualTo(TaskState.TO_DO);
		assertThat(read.priority()).isEqualTo(TaskPriority.HIGH);
	}

	@Test
	void eachFieldIsStoredInItsColumn() {
		Task stored = store("Write tests", "Text", TaskPriority.LOW);

		assertThat(this.jdbc.sql("SELECT title, description, state, priority, deleted_at FROM task WHERE task_id = ?")
			.param(stored.id().value())
			.query((row, number) -> List.of(row.getString(1), row.getString(2), row.getString(3), row.getString(4),
					String.valueOf(row.getObject(5))))
			.single()).containsExactly("Write tests", "Text", "TO_DO", "LOW", "null");
	}

	@Test
	void noDescriptionIsStoredAsNullAndReadBackAsNone() {
		Task stored = store("Plain", null, TaskPriority.MEDIUM);

		Task read = this.transactions.execute((transaction) -> this.tasks.findById(this.project, stored.id()).orElseThrow());

		assertThat(read.description()).isEmpty();
		assertThat(this.jdbc.sql("SELECT count(*) FROM task WHERE task_id = ? AND description IS NULL")
			.param(stored.id().value())
			.query(Integer.class)
			.single()).isEqualTo(1);
	}

	@Test
	void anEditAndAStateChangeAreStored() {
		Task stored = store("Old", "Text", TaskPriority.LOW);

		this.transactions.executeWithoutResult((transaction) -> {
			Task task = this.tasks.findById(this.project, stored.id()).orElseThrow();
			task.edit(new TaskEdit(new TaskTitle("New"), true, null, TaskPriority.HIGH));
			task.changeState(TaskState.DONE);
		});

		assertThat(this.jdbc.sql("SELECT title, state, priority FROM task WHERE task_id = ? AND description IS NULL")
			.param(stored.id().value())
			.query((row, number) -> List.of(row.getString(1), row.getString(2), row.getString(3)))
			.single()).containsExactly("New", "DONE", "HIGH");
	}

	@Test
	void unfinishedMeansAToDoOrInProgressTaskThatIsNotDeleted() {
		assertThat(hasUnfinished()).as("no tasks").isFalse();
		Task task = store("Only", null, TaskPriority.MEDIUM);
		assertThat(hasUnfinished()).as("To do").isTrue();

		change(task, TaskState.IN_PROGRESS);
		assertThat(hasUnfinished()).as("In progress").isTrue();

		change(task, TaskState.DONE);
		assertThat(hasUnfinished()).as("Done").isFalse();

		Task deleted = store("Deleted", null, TaskPriority.MEDIUM);
		this.jdbc.sql("UPDATE task SET deleted_at = now() WHERE task_id = ?").param(deleted.id().value()).update();
		assertThat(hasUnfinished()).as("a deleted To do task").isFalse();
	}

	@Test
	void aTaskOfAnotherProjectIsNotFound() {
		Task stored = store("Mine", null, TaskPriority.MEDIUM);

		assertThat(this.tasks.findById(ProjectId.generate(), stored.id())).isEmpty();
	}

	@Test
	void theListIsInCreationOrderFilteredAndPaged() {
		Task first = store("First", null, TaskPriority.LOW);
		Task second = store("Second", null, TaskPriority.HIGH);
		Task third = store("Third", null, TaskPriority.HIGH);
		change(second, TaskState.IN_PROGRESS);
		change(third, TaskState.DONE);

		assertThat(titles(this.tasks.find(this.project, Set.of(), Set.of(), 1, 20))).containsExactly("First", "Second",
				"Third");
		assertThat(titles(this.tasks.find(this.project, Set.of(), Set.of(TaskPriority.HIGH), 1, 20)))
			.containsExactly("Second", "Third");
		assertThat(titles(this.tasks.find(this.project, Set.of(TaskState.TO_DO, TaskState.DONE), Set.of(), 1, 20)))
			.containsExactly("First", "Third");
		assertThat(titles(this.tasks.find(this.project, Set.of(TaskState.DONE), Set.of(TaskPriority.LOW), 1, 20)))
			.isEmpty();
		TaskPage page = this.tasks.find(this.project, Set.of(), Set.of(), 2, 2);
		assertThat(titles(page)).containsExactly("Third");
		assertThat(page.totalItems()).isEqualTo(3);
		assertThat(this.tasks.find(this.project, Set.of(), Set.of(), 5, 2).items()).isEmpty();
		assertThat(this.tasks.find(this.project, Set.of(), Set.of(), 5, 2).totalItems()).isEqualTo(3);
		assertThat(first.id()).isNotEqualTo(second.id());
	}

	@Test
	void theListOfAProjectHoldsNoTaskOfAnotherProject() {
		store("Mine", null, TaskPriority.MEDIUM);

		assertThat(this.tasks.find(ProjectId.generate(), Set.of(), Set.of(), 1, 20).items()).isEmpty();
	}

	@Test
	void aDeletedTaskIsNotFoundAndNotListedAndLosesItsAssignmentsButItsRowStays() {
		Task stored = store("Doomed", null, TaskPriority.MEDIUM);
		store("Kept", null, TaskPriority.MEDIUM);
		this.jdbc.sql("INSERT INTO assignment (task_id, person_id, project_id) VALUES (?, ?, ?)")
			.params(stored.id().value(), this.owner.value(), this.project.value())
			.update();

		this.transactions.executeWithoutResult((transaction) -> this.tasks.findById(this.project, stored.id())
			.orElseThrow()
			.delete(java.time.Instant.now()));

		assertThat(this.tasks.findById(this.project, stored.id())).isEmpty();
		TaskPage page = this.tasks.find(this.project, Set.of(), Set.of(), 1, 20);
		assertThat(titles(page)).containsExactly("Kept");
		assertThat(page.totalItems()).isEqualTo(1);
		assertThat(this.jdbc.sql("SELECT count(*) FROM task WHERE task_id = ? AND deleted_at IS NOT NULL")
			.param(stored.id().value())
			.query(Integer.class)
			.single()).isEqualTo(1);
		assertThat(this.jdbc.sql("SELECT count(*) FROM assignment WHERE task_id = ?")
			.param(stored.id().value())
			.query(Integer.class)
			.single()).isZero();
	}

	private Task store(String title, String description, TaskPriority priority) {
		return this.transactions.execute((transaction) -> this.tasks.save(Task.create(TaskId.generate(), this.project,
				new TaskTitle(title), TaskDescription.ofNullable(description), priority)));
	}

	private boolean hasUnfinished() {
		return Boolean.TRUE.equals(this.transactions.execute((transaction) -> this.tasks.hasUnfinished(this.project)));
	}

	private void change(Task task, TaskState state) {
		this.transactions.executeWithoutResult(
				(transaction) -> this.tasks.findById(this.project, task.id()).orElseThrow().changeState(state));
	}

	private List<String> titles(TaskPage page) {
		return page.items().stream().map((task) -> task.title().value()).toList();
	}

}
