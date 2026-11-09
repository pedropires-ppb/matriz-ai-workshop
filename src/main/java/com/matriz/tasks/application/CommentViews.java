package com.matriz.tasks.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.matriz.identity.PersonDirectory;
import com.matriz.identity.PersonId;
import com.matriz.tasks.domain.Comment;

import org.springframework.stereotype.Component;

/**
 * Makes the details of comments, with the display names of their authors, which Identity gives
 * (openapi.yaml, Comment). A comment keeps its author when the author is no longer a member.
 */
@Component
class CommentViews {

	private final PersonDirectory people;

	CommentViews(PersonDirectory people) {
		this.people = people;
	}

	CommentDetails of(Comment comment) {
		return of(List.of(comment)).get(0);
	}

	List<CommentDetails> of(Collection<Comment> comments) {
		Set<PersonId> authors = comments.stream().map(Comment::author).collect(Collectors.toSet());
		Map<PersonId, String> names = this.people.displayNames(authors);
		return comments.stream()
			.map((comment) -> new CommentDetails(comment.id(), comment.taskId(), comment.author(),
					names.getOrDefault(comment.author(), comment.author().toString()), comment.createdAt(),
					comment.text().value()))
			.toList();
	}

}
