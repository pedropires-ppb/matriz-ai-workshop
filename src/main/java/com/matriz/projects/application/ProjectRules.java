package com.matriz.projects.application;

import java.util.ArrayList;
import java.util.List;

import com.matriz.projects.AccessDecision;
import com.matriz.projects.application.InvalidProjectException.InvalidField;
import com.matriz.projects.domain.ProjectDescription;
import com.matriz.projects.domain.ProjectName;

/**
 * What the use cases share: turning the access policy's decisions into the exceptions of ADR-004,
 * and checking the texts of Assumption A-031.
 */
final class ProjectRules {

	static final String NAME = "/name";

	static final String DESCRIPTION = "/description";

	private ProjectRules() {
	}

	/** Refuses with 404 or 403, which come before any check of the content (ADR-004 decision 5). */
	static void requireMemberWithPermission(AccessDecision decision) {
		switch (decision) {
			case NOT_FOUND -> throw new ProjectNotFoundException();
			case FORBIDDEN -> throw new NotProjectOwnerException();
			case ALLOWED, CONFLICT -> {
			}
		}
	}

	/** Refuses with 409, which comes after the check of the content (ADR-004 decision 5). */
	static void requireNoConflict(AccessDecision decision) {
		if (decision == AccessDecision.CONFLICT) {
			throw new ProjectCompletedException();
		}
	}

	/** The name, or {@code null} if it is {@code null}, with its problem added to the list if it is invalid. */
	static ProjectName name(String text, List<InvalidField> invalid) {
		try {
			return text == null ? null : new ProjectName(text);
		}
		catch (IllegalArgumentException ex) {
			invalid.add(new InvalidField(NAME, ex.getMessage()));
			return null;
		}
	}

	/** The description, or {@code null} for none, with its problem added to the list if it is invalid. */
	static ProjectDescription description(String text, List<InvalidField> invalid) {
		try {
			return ProjectDescription.ofNullable(text);
		}
		catch (IllegalArgumentException ex) {
			invalid.add(new InvalidField(DESCRIPTION, ex.getMessage()));
			return null;
		}
	}

	static List<InvalidField> noProblems() {
		return new ArrayList<>();
	}

	static void requireNoProblems(List<InvalidField> invalid) {
		if (!invalid.isEmpty()) {
			throw new InvalidProjectException(invalid);
		}
	}

}
