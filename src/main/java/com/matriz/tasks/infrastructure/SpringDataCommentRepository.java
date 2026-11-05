package com.matriz.tasks.infrastructure;

import java.util.List;
import java.util.UUID;

import com.matriz.tasks.CommentId;
import com.matriz.tasks.domain.Comment;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA's side of {@link JpaCommentRepository} (ADR-008 decision 4). It offers no way to
 * change or remove a comment (rule 7).
 */
interface SpringDataCommentRepository extends Repository<Comment, CommentId> {

	Comment save(Comment comment);

	@Query("select c from Comment c where c.taskId.value = :task order by c.createdAt, c.creationOrder")
	List<Comment> findOldestFirst(@Param("task") UUID task);

}
