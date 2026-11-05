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
 * aggregate (ADR-008 decision 2). Until I7 it is only read, and removed with its task (Assumption
 * A-022); assignments are created by I7. Two assignments are equal when they have the same
 * identifier.
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
