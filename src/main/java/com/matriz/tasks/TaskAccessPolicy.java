package com.matriz.tasks;

import java.util.Optional;

import com.matriz.projects.AccessDecision;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectState;

/**
 * Whether a person may act on the tasks of a project that exists and is not deleted (ADR-008, open
 * point "Layers of Progress and Access control"; module rule 3). Tasks' use cases call it. The Access
 * control module implements it, and decides only from the facts it is given: the person's role in the
 * project, the project's state, and, for the actions that depend on them, whether the person is an
 * assignee of the task or acts on their own assignment ({@link TaskFacts}).
 */
public interface TaskAccessPolicy {

	/**
	 * The decision for this action. The checks come in the order of ADR-004 decision 5: no role is
	 * {@link AccessDecision#NOT_FOUND}, then {@link AccessDecision#FORBIDDEN}, then
	 * {@link AccessDecision#CONFLICT}.
	 * @param action what the person asks to do
	 * @param state the state of the task's project
	 * @param role the person's role in the project, or empty if the person is not a member
	 * @param facts what is known about the task and the request
	 */
	AccessDecision decide(TaskAction action, ProjectState state, Optional<MemberRole> role, TaskFacts facts);

	/** The decision for an action that does not depend on the task: see {@link TaskFacts#NONE}. */
	default AccessDecision decide(TaskAction action, ProjectState state, Optional<MemberRole> role) {
		return decide(action, state, role, TaskFacts.NONE);
	}

}
