package com.matriz.tasks.application;

import java.time.Instant;

import com.matriz.identity.PersonId;
import com.matriz.tasks.CommentId;
import com.matriz.tasks.TaskId;

/**
 * A comment as shown to a member (openapi.yaml, Comment).
 * @param author the person who added it, from the token when it was added
 * @param authorName the author's display name
 * @param createdAt the server's date and time when it was added
 */
public record CommentDetails(CommentId commentId, TaskId taskId, PersonId author, String authorName,
		Instant createdAt, String text) {

}
