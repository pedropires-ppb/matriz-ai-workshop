package com.matriz.projects.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.matriz.identity.PersonId;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectId;
import com.matriz.projects.ProjectState;

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
 * A project, with its Memberships (glossary). The aggregate of the Projects module (ADR-008
 * decision 2): it is stored, loaded, and locked as a whole (ADR-006). Rules checked here:
 * <ul>
 * <li>A project starts Active, and its creator is its single Project Owner and a member (A-008).</li>
 * <li>Only the Project Owner edits, deletes, or completes it, and only while it is Active (A-001, A-011).</li>
 * <li>It is completed only when none of its tasks is To do or In progress (A-007).</li>
 * <li>Only the Project Owner adds or removes Project Participants, only while it is Active; the Project
 * Owner, and a member assigned to a task, cannot be removed (A-009).</li>
 * <li>Deleting sets a deletion time; the data is kept (A-022).</li>
 * </ul>
 * The access policy decides first, and with the answers of the API; the checks here guard the rules
 * against any caller that skips it.
 */
@Entity
@Table(name = "project")
public class Project {

	@EmbeddedId
	private ProjectId id;

	@Embedded
	private ProjectName name;

	/** {@code null} when the project has no description. */
	@Embedded
	private ProjectDescription description;

	@Enumerated(EnumType.STRING)
	@Column(name = "state", nullable = false)
	private ProjectState state;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "project_id", insertable = false, updatable = false)
	private Set<Membership> memberships = new HashSet<>();

	private Project(ProjectId id, ProjectName name, ProjectDescription description) {
		this.id = Objects.requireNonNull(id, "A project identifier is required.");
		this.name = Objects.requireNonNull(name, "A name is required.");
		this.description = description;
		this.state = ProjectState.ACTIVE;
	}

	/** For Jakarta Persistence only. */
	protected Project() {
	}

	/** An Active project that the owner has created and belongs to as its Project Owner. */
	public static Project create(ProjectId id, ProjectName name, ProjectDescription description, PersonId owner) {
		Project project = new Project(id, name, description);
		project.memberships.add(new Membership(id, Objects.requireNonNull(owner, "An owner is required."),
				MemberRole.PROJECT_OWNER));
		return project;
	}

	public ProjectId id() {
		return this.id;
	}

	public ProjectName name() {
		return this.name;
	}

	/** The description, or empty if the project has none. */
	public Optional<ProjectDescription> description() {
		return Optional.ofNullable(this.description);
	}

	public ProjectState state() {
		return this.state;
	}

	public boolean isDeleted() {
		return this.deletedAt != null;
	}

	/** The person's role in this project, or empty if the person is not a member. */
	public Optional<MemberRole> roleOf(PersonId person) {
		return this.memberships.stream()
			.filter((membership) -> membership.personId().equals(person))
			.map(Membership::role)
			.findFirst();
	}

	/** Everyone in the project with their role, in no particular order. */
	public Set<Membership> members() {
		return Collections.unmodifiableSet(this.memberships);
	}

	/**
	 * Applies the edit: the name, and the description, if they are given. Editing to the current
	 * values changes nothing.
	 * @throws IllegalStateException if the person is not the Project Owner, or the project is not
	 * Active or is deleted
	 */
	public void edit(PersonId actor, ProjectEdit edit) {
		requireOwnerOfActiveProject(actor);
		Objects.requireNonNull(edit, "An edit is required.");
		if (edit.name() != null) {
			this.name = edit.name();
		}
		if (edit.changesDescription()) {
			this.description = edit.description();
		}
	}

	/**
	 * Deletes the project: it is hidden from everyone from now on, and its data is kept.
	 * @throws IllegalStateException if the person is not the Project Owner, or the project is not
	 * Active or is already deleted
	 */
	public void delete(PersonId actor, Instant when) {
		requireOwnerOfActiveProject(actor);
		this.deletedAt = Objects.requireNonNull(when, "A deletion time is required.");
	}

	/**
	 * Completes the project. Completing cannot be undone (OQ-006).
	 * @param hasUnfinishedTasks whether any of its tasks, not deleted, is To do or In progress
	 * @throws IllegalStateException if the person is not the Project Owner, the project is not Active
	 * or is deleted, or it has unfinished tasks
	 */
	public void complete(PersonId actor, boolean hasUnfinishedTasks) {
		requireOwnerOfActiveProject(actor);
		if (hasUnfinishedTasks) {
			throw new IllegalStateException("The project has unfinished tasks.");
		}
		this.state = ProjectState.COMPLETED;
	}

	/**
	 * Adds the person as a Project Participant; the role cannot be chosen (OQ-002, OQ-008).
	 * @throws IllegalStateException if the actor is not the Project Owner, the project is not Active or
	 * is deleted, or the person is already a member
	 */
	public void addParticipant(PersonId actor, PersonId person) {
		requireOwnerOfActiveProject(actor);
		Objects.requireNonNull(person, "A person is required.");
		if (roleOf(person).isPresent()) {
			throw new IllegalStateException("The person is already a member.");
		}
		this.memberships.add(new Membership(this.id, person, MemberRole.PROJECT_PARTICIPANT));
	}

	/**
	 * Removes a Project Participant, who can then no longer view the project (FR-005).
	 * @param hasAssignedTasks whether the person is assigned to a task of the project that is not deleted
	 * @throws IllegalStateException if the actor is not the Project Owner, the project is not Active or
	 * is deleted, or the person is not a member, is the Project Owner, or has assigned tasks (A-009)
	 */
	public void removeParticipant(PersonId actor, PersonId person, boolean hasAssignedTasks) {
		requireOwnerOfActiveProject(actor);
		MemberRole role = roleOf(person).orElseThrow(() -> new IllegalStateException("The person is not a member."));
		if (role == MemberRole.PROJECT_OWNER) {
			throw new IllegalStateException("The Project Owner cannot be removed.");
		}
		if (hasAssignedTasks) {
			throw new IllegalStateException("The person is assigned to tasks of the project.");
		}
		this.memberships.removeIf((membership) -> membership.personId().equals(person));
	}

	private void requireOwnerOfActiveProject(PersonId actor) {
		if (isDeleted()) {
			throw new IllegalStateException("The project is deleted.");
		}
		if (roleOf(actor).orElse(null) != MemberRole.PROJECT_OWNER) {
			throw new IllegalStateException("Only the Project Owner can change the project.");
		}
		if (this.state != ProjectState.ACTIVE) {
			throw new IllegalStateException("The project is not Active.");
		}
	}

	@Override
	public String toString() {
		return "Project[" + this.id + "]";
	}

}
