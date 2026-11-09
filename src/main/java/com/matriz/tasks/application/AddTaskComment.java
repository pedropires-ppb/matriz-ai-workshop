package com.matriz.tasks.application;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.matriz.identity.PersonId;
import com.matriz.projects.ProjectAccess;
import com.matriz.projects.ProjectAccess.ProjectFacts;
import com.matriz.projects.ProjectId;
import com.matriz.tasks.CommentId;
import com.matriz.tasks.TaskAccessPolicy;
import com.matriz.tasks.TaskAction;
import com.matriz.tasks.TaskId;
import com.matriz.tasks.application.InvalidTaskException.InvalidField;
import com.matriz.tasks.domain.Comment;
import com.matriz.tasks.domain.CommentRepository;
import com.matriz.tasks.domain.CommentText;
import com.matriz.tasks.domain.Task;
import com.matriz.tasks.domain.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds a comment to a task (FR-020; US-021; Assumptions A-023, A-036). Any member may comment, also
 * when the project is Completed (rule 5). The author is the person of the token and the date and time
 * are the server's, never the client's. The checks come in the order {@code 404}, {@code 422}; there
 * is no {@code 403} and no {@code 409}. The project's row is locked for the whole use case (ADR-006).
 */
@Service
public class AddTaskComment {

	static final String TEXT = "/text";

	static final String INVALID = "The comment is invalid.";

	private final ProjectAccess projects;

	private final TaskRepository tasks;

	private final CommentRepository comments;

	private final TaskAccessPolicy policy;

	private final CommentViews views;

	private final Clock clock;

	AddTaskComment(ProjectAccess projects, TaskRepository tasks, CommentRepository comments, TaskAccessPolicy policy,
			CommentViews views, Clock clock) {
		this.projects = projects;
		this.tasks = tasks;
		this.comments = comments;
		this.policy = policy;
		this.views = views;
		this.clock = clock;
	}

	/**
	 * @param person the author, from the token
	 * @param text the text, as sent
	 * @throws TaskNotFoundException if there is no such project or task, or the person is not a member
	 * @throws InvalidTaskException if the text is empty after trimming or has more than 2000 characters
	 */
	@Transactional
	public CommentDetails add(PersonId person, ProjectId project, TaskId id, String text) {
		ProjectFacts facts = this.projects.findForUpdate(project, person).orElseThrow(TaskNotFoundException::ofTask);
		TaskRules.requireMemberForTask(this.policy.decide(TaskAction.COMMENT, facts.state(), facts.role()));
		Task task = this.tasks.findById(project, id).orElseThrow(TaskNotFoundException::ofTask);
		CommentText valid;
		try {
			valid = new CommentText(text);
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidTaskException(INVALID, List.of(new InvalidField(TEXT, ex.getMessage())));
		}
		Comment comment = Comment.add(CommentId.generate(), task.id(), person, valid,
				this.clock.instant().truncatedTo(ChronoUnit.MILLIS));
		return this.views.of(this.comments.save(comment));
	}

}
