package com.matriz.tasks.domain;

import java.time.Instant;
import java.util.Objects;

import com.matriz.identity.PersonId;
import com.matriz.tasks.CommentId;
import com.matriz.tasks.TaskId;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A comment on a task (glossary: Comment; ADR-008 decision 2). An aggregate of its own, which refers
 * to its task and its author by identifier. Rule 7: a comment is never changed, so the class has no
 * method that changes it. Who may comment, and in which state of the project, is decided before, by
 * the access policy. The technical order of creation is never read through the class: only the
 * repository's query sorts by it (ADR-008).
 */
@Entity
@Table(name = "comment")
public class Comment {

	@EmbeddedId
	private CommentId id;

	@Embedded
	private TaskId taskId;

	@Embedded
	@AttributeOverride(name = "value", column = @Column(name = "author_person_id", nullable = false))
	private PersonId author;

	@Embedded
	private CommentText text;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	/** Given by the database; only the query reads it. Never used by the class, and without an accessor. */
	@Column(name = "creation_order", insertable = false, updatable = false)
	private Long creationOrder;

	private Comment(CommentId id, TaskId taskId, PersonId author, CommentText text, Instant createdAt) {
		this.id = Objects.requireNonNull(id, "A comment identifier is required.");
		this.taskId = Objects.requireNonNull(taskId, "A task identifier is required.");
		this.author = Objects.requireNonNull(author, "An author is required.");
		this.text = Objects.requireNonNull(text, "A text is required.");
		this.createdAt = Objects.requireNonNull(createdAt, "A date and time are required.");
	}

	/** For Jakarta Persistence only. */
	protected Comment() {
	}

	/** A new comment of the task, by the author, added at that time. */
	public static Comment add(CommentId id, TaskId taskId, PersonId author, CommentText text, Instant createdAt) {
		return new Comment(id, taskId, author, text, createdAt);
	}

	public CommentId id() {
		return this.id;
	}

	public TaskId taskId() {
		return this.taskId;
	}

	public PersonId author() {
		return this.author;
	}

	public CommentText text() {
		return this.text;
	}

	public Instant createdAt() {
		return this.createdAt;
	}

	@Override
	public String toString() {
		return "Comment[" + this.id + "]";
	}

}
