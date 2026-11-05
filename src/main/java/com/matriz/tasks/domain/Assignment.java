package com.matriz.tasks.domain;

import java.util.Objects;

import com.matriz.identity.PersonId;
import com.matriz.projects.ProjectId;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A person's assignment to a task (glossary: Assignment). An entity inside the {@link Task}
 * aggregate (ADR-008 decision 2). It is created by assigning a person to the task (I7), and removed
 * with it, or by unassigning the person (Assumptions A-022, A-035). Two assignments are equal when
 * they have the same identifier.
 */
@Entity
@Table(name = "assignment")
public class Assignment {

	@EmbeddedId
	private AssignmentId id;

	/** The task's project, which the database checks against the person's membership. */
	@Embedded
	private ProjectId projectId;

	/** For Jakarta Persistence only. */
	protected Assignment() {
	}

	Assignment(AssignmentId id, ProjectId projectId) {
		this.id = Objects.requireNonNull(id, "An assignment identifier is required.");
		this.projectId = Objects.requireNonNull(projectId, "A project identifier is required.");
	}

	public PersonId personId() {
		return this.id.personId();
	}

	@Override
	public boolean equals(Object other) {
		return this == other || (other instanceof Assignment assignment && Objects.equals(this.id, assignment.id));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.id);
	}

	@Override
	public String toString() {
		return "Assignment[" + this.id + "]";
	}

}
