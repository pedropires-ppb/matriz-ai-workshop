package com.matriz.tasks.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.matriz.identity.PersonId;
import com.matriz.projects.ProjectId;
import com.matriz.tasks.TaskId;
import com.matriz.tasks.TaskPriority;
import com.matriz.tasks.TaskState;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * A task, with its Assignments (glossary). The aggregate of the Tasks module (ADR-008 decision 2),
 * which belongs to exactly one project. Rules checked here:
 * <ul>
 * <li>A task starts To do with the priority chosen, Medium unless another is given (A-010, A-021).</li>
 * <li>Any state can be set from any state (A-005).</li>
 * <li>Deleting sets a deletion time and removes the task's Assignments; the data is kept (A-022).</li>
 * </ul>
 * Who may do this, and in which state of the project, is decided before, by the access policy and
 * with the project's state; this class does not know the project's members. The technical order of
 * creation is never read through the class: only the repository's queries sort by it (ADR-008).
 */
@Entity
@Table(name = "task")
public class Task {

	@EmbeddedId
	private TaskId id;

	@Embedded
	private ProjectId projectId;

	@Embedded
	private TaskTitle title;

	/** {@code null} when the task has no description. */
	@Embedded
	private TaskDescription description;

	@Enumerated(EnumType.STRING)
	@Column(name = "state", nullable = false)
	private TaskState state;

	@Enumerated(EnumType.STRING)
	@Column(name = "priority", nullable = false)
	private TaskPriority priority;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	/** Given by the database; only the queries read it. Never used by the class, and without an accessor. */
	@Column(name = "creation_order", insertable = false, updatable = false)
	private Long creationOrder;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id", insertable = false, updatable = false)
	private Set<Assignment> assignments = new HashSet<>();

	private Task(TaskId id, ProjectId projectId, TaskTitle title, TaskDescription description,
			TaskPriority priority) {
		this.id = Objects.requireNonNull(id, "A task identifier is required.");
		this.projectId = Objects.requireNonNull(projectId, "A project identifier is required.");
		this.title = Objects.requireNonNull(title, "A title is required.");
		this.description = description;
		this.priority = Objects.requireNonNull(priority, "A priority is required.");
		this.state = TaskState.TO_DO;
	}

	/** For Jakarta Persistence only. */
	protected Task() {
	}

	/**
	 * A To do task of the project, with no assignee.
	 * @param description the description, or {@code null} for none
	 */
	public static Task create(TaskId id, ProjectId projectId, TaskTitle title, TaskDescription description,
			TaskPriority priority) {
		return new Task(id, projectId, title, description, priority);
	}

	public TaskId id() {
		return this.id;
	}

	public ProjectId projectId() {
		return this.projectId;
	}

	public TaskTitle title() {
		return this.title;
	}

	/** The description, or empty if the task has none. */
	public Optional<TaskDescription> description() {
		return Optional.ofNullable(this.description);
	}

	public TaskState state() {
		return this.state;
	}

	public TaskPriority priority() {
		return this.priority;
	}

	public boolean isDeleted() {
		return this.deletedAt != null;
	}

	/** The people assigned to the task. */
	public Set<PersonId> assignees() {
		return this.assignments.stream().map(Assignment::personId).collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Applies the edit: the title, the description, and the priority, if they are given. Editing to the
	 * current values changes nothing.
	 * @throws IllegalStateException if the task is deleted
	 */
	public void edit(TaskEdit edit) {
		requireNotDeleted();
		Objects.requireNonNull(edit, "An edit is required.");
		if (edit.title() != null) {
			this.title = edit.title();
		}
		if (edit.changesDescription()) {
			this.description = edit.description();
		}
		if (edit.priority() != null) {
			this.priority = edit.priority();
		}
	}

	/**
	 * Sets the state, from any state to any state, including the one it has.
	 * @throws IllegalStateException if the task is deleted
	 */
	public void changeState(TaskState newState) {
		requireNotDeleted();
		this.state = Objects.requireNonNull(newState, "A state is required.");
	}

	/**
	 * Deletes the task: it is hidden from everyone from now on, its Assignments are removed, and
	 * its other data is kept.
	 * @throws IllegalStateException if the task is already deleted
	 */
	public void delete(Instant when) {
		requireNotDeleted();
		this.deletedAt = Objects.requireNonNull(when, "A deletion time is required.");
		this.assignments.clear();
	}

	private void requireNotDeleted() {
		if (isDeleted()) {
			throw new IllegalStateException("The task is deleted.");
		}
	}

	@Override
	public String toString() {
		return "Task[" + this.id + "]";
	}

}
