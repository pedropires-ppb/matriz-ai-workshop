package com.matriz.tasks.application;

import java.util.List;

import com.matriz.identity.PersonId;
import com.matriz.projects.ProjectAccess;
import com.matriz.projects.ProjectAccess.ProjectFacts;
import com.matriz.projects.ProjectId;
import com.matriz.tasks.TaskAccessPolicy;
import com.matriz.tasks.TaskAction;
import com.matriz.tasks.TaskId;
import com.matriz.tasks.domain.CommentRepository;
import com.matriz.tasks.domain.Task;
import com.matriz.tasks.domain.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lists the comments of a task, oldest first, to a member of its project, also in a Completed project
 * (FR-021; US-022; Assumptions A-011, A-036). The list is whole and may be empty.
 */
@Service
public class ListTaskComments {

	private final ProjectAccess projects;

	private final TaskRepository tasks;

	private final CommentRepository comments;

	private final TaskAccessPolicy policy;

	private final CommentViews views;

	ListTaskComments(ProjectAccess projects, TaskRepository tasks, CommentRepository comments,
			TaskAccessPolicy policy, CommentViews views) {
		this.projects = projects;
		this.tasks = tasks;
		this.comments = comments;
		this.policy = policy;
		this.views = views;
	}

	/** @throws TaskNotFoundException if there is no such project or task, or the person is not a member */
	@Transactional(readOnly = true)
	public List<CommentDetails> list(PersonId person, ProjectId project, TaskId id) {
		ProjectFacts facts = this.projects.find(project, person).orElseThrow(TaskNotFoundException::ofTask);
		TaskRules.requireMemberForTask(this.policy.decide(TaskAction.VIEW, facts.state(), facts.role()));
		Task task = this.tasks.findById(project, id).orElseThrow(TaskNotFoundException::ofTask);
		return this.views.of(this.comments.findByTask(task.id()));
	}

}
