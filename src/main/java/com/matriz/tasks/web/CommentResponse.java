package com.matriz.tasks.web;

import com.matriz.tasks.application.CommentDetails;

/** A comment (openapi.yaml, Comment). {@code createdAt} is RFC 3339, in UTC. */
record CommentResponse(String commentId, String taskId, Author author, String createdAt, String text) {

	static CommentResponse of(CommentDetails comment) {
		return new CommentResponse(comment.commentId().toString(), comment.taskId().toString(),
				new Author(comment.author().toString(), comment.authorName()), comment.createdAt().toString(),
				comment.text());
	}

	/** The person who added a comment (openapi.yaml, PersonSummary). */
	record Author(String personId, String displayName) {

	}

}
