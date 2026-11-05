package com.matriz.tasks;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Identifies a task (ADR-008 decision 2). Other modules refer to a task only by this identifier, so
 * it is in the Tasks API package. The column is {@code task_id}.
 */
@Embeddable
public record TaskId(@Column(name = "task_id") UUID value) implements Serializable {

	public TaskId {
		Objects.requireNonNull(value, "A task identifier is required.");
	}

	/** A new, unused identifier. */
	public static TaskId generate() {
		return new TaskId(UUID.randomUUID());
	}

	@Override
	public String toString() {
		return this.value.toString();
	}

}
