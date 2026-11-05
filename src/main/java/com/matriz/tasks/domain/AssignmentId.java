package com.matriz.tasks.domain;

import java.io.Serializable;
import java.util.Objects;

import com.matriz.identity.PersonId;
import com.matriz.tasks.TaskId;

import jakarta.persistence.Embeddable;

/** Identifies an assignment: one person assigned to one task (ADR-008, mapping of Assignments). */
@Embeddable
public record AssignmentId(TaskId taskId, PersonId personId) implements Serializable {

	public AssignmentId {
		Objects.requireNonNull(taskId, "A task identifier is required.");
		Objects.requireNonNull(personId, "A person identifier is required.");
	}

}
