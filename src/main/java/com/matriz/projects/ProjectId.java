package com.matriz.projects;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Identifies a project (ADR-008 decision 2). Other modules refer to a project only by this
 * identifier, so it is in the Projects API package. The column is {@code project_id}.
 */
@Embeddable
public record ProjectId(@Column(name = "project_id") UUID value) implements Serializable {

	public ProjectId {
		Objects.requireNonNull(value, "A project identifier is required.");
	}

	/** A new, unused identifier. */
	public static ProjectId generate() {
		return new ProjectId(UUID.randomUUID());
	}

	@Override
	public String toString() {
		return this.value.toString();
	}

}
