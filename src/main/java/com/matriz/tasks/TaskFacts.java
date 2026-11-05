package com.matriz.tasks;

/**
 * What the access policy needs to know about the task and the request, besides the person's role and
 * the project's state (Assumptions A-019, A-020, A-035).
 *
 * @param assignee whether the person is an assignee of the task
 * @param ownAssignment whether the request assigns the person to the task, or removes the person
 * from it, and not someone else
 */
public record TaskFacts(boolean assignee, boolean ownAssignment) {

	/** No fact beyond the role and the state: for an action that does not depend on the task. */
	public static final TaskFacts NONE = new TaskFacts(false, false);

}
