package com.matriz.tasks;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Identifies a comment (ADR-008 decision 2). The column is {@code comment_id}. */
@Embeddable
public record CommentId(@Column(name = "comment_id") UUID value) implements Serializable {

	public CommentId {
		Objects.requireNonNull(value, "A comment identifier is required.");
	}

	/** A new, unused identifier. */
	public static CommentId generate() {
		return new CommentId(UUID.randomUUID());
	}

	@Override
	public String toString() {
		return this.value.toString();
	}

}
