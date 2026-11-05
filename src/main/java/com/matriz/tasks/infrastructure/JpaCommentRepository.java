package com.matriz.tasks.infrastructure;

import java.util.List;

import com.matriz.tasks.TaskId;
import com.matriz.tasks.domain.Comment;
import com.matriz.tasks.domain.CommentRepository;

import org.springframework.stereotype.Repository;

/** {@link CommentRepository} with Spring Data JPA (ADR-008 decision 4). */
@Repository
class JpaCommentRepository implements CommentRepository {

	private final SpringDataCommentRepository comments;

	JpaCommentRepository(SpringDataCommentRepository comments) {
		this.comments = comments;
	}

	@Override
	public Comment save(Comment comment) {
		return this.comments.save(comment);
	}

	@Override
	public List<Comment> findByTask(TaskId task) {
		return this.comments.findOldestFirst(task.value());
	}

}
