package com.matriz.tasks.application;

import java.util.ArrayList;
import java.util.List;

import com.matriz.projects.AccessDecision;
import com.matriz.tasks.TaskPriority;
import com.matriz.tasks.TaskState;
import com.matriz.tasks.application.InvalidTaskException.InvalidField;
import com.matriz.tasks.domain.TaskDescription;
import com.matriz.tasks.domain.TaskTitle;

/**
 * What the use cases share: turning the access policy's decisions into the exceptions of ADR-004,
 * and checking the texts and the codes of Assumption A-033.
 */
final class TaskRules {

	static final String TITLE = "/title";

	static final String DESCRIPTION = "/description";

	static final String PRIORITY = "/priority";

	static final String STATE = "/state";

	static final String PRIORITY_CODES = "Use one of LOW, MEDIUM, or HIGH.";

	static final String STATE_CODES = "Must be TO_DO, IN_PROGRESS, or DONE.";

	private TaskRules() {
	}

	/** Refuses with 404 if the person is not a member of the project, for an operation on its tasks. */
	static void requireMemberOfProject(AccessDecision decision) {
		if (decision == AccessDecision.NOT_FOUND) {
			throw TaskNotFoundException.ofProject();
		}
	}

	/** Refuses with 404 if the person is not a member of the project, for an operation on one task. */
	static void requireMemberForTask(AccessDecision decision) {
		if (decision == AccessDecision.NOT_FOUND) {
			throw TaskNotFoundException.ofTask();
		}
	}

	/** Refuses with 403, which comes after the 404 and before any check of the content (ADR-004 decision 5). */
	static void requirePermission(AccessDecision decision) {
		if (decision == AccessDecision.FORBIDDEN) {
			throw new NotProjectOwnerException();
		}
	}

	/** Refuses with 409, which comes after the check of the content (ADR-004 decision 5). */
	static void requireNoConflict(AccessDecision decision) {
		if (decision == AccessDecision.CONFLICT) {
			throw new ProjectCompletedException();
		}
	}

	/** The title, or {@code null} if it is {@code null}, with its problem added to the list if it is invalid. */
	static TaskTitle title(String text, List<InvalidField> invalid) {
		try {
			return text == null ? null : new TaskTitle(text);
		}
		catch (IllegalArgumentException ex) {
			invalid.add(new InvalidField(TITLE, ex.getMessage()));
			return null;
		}
	}

	/** The description, or {@code null} for none, with its problem added to the list if it is invalid. */
	static TaskDescription description(String text, List<InvalidField> invalid) {
		try {
			return TaskDescription.ofNullable(text);
		}
		catch (IllegalArgumentException ex) {
			invalid.add(new InvalidField(DESCRIPTION, ex.getMessage()));
			return null;
		}
	}

	/** The priority with this code, or {@code null} if the code is {@code null}; an unknown code is added to the list. */
	static TaskPriority priority(String code, List<InvalidField> invalid) {
		if (code == null) {
			return null;
		}
		try {
			return TaskPriority.valueOf(code);
		}
		catch (IllegalArgumentException ex) {
			invalid.add(new InvalidField(PRIORITY, PRIORITY_CODES));
			return null;
		}
	}

	/** The state with this code; a missing or unknown code is added to the list. */
	static TaskState state(String code, List<InvalidField> invalid) {
		try {
			if (code != null) {
				return TaskState.valueOf(code);
			}
		}
		catch (IllegalArgumentException ex) {
			// Falls through to the same refusal as a missing code.
		}
		invalid.add(new InvalidField(STATE, STATE_CODES));
		return null;
	}

	static List<InvalidField> noProblems() {
		return new ArrayList<>();
	}

	static void requireNoProblems(List<InvalidField> invalid) {
		if (!invalid.isEmpty()) {
			throw new InvalidTaskException(invalid);
		}
	}

}
