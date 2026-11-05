package com.matriz.projects;

import java.util.Optional;

/**
 * Whether a person may act on a project that exists and is not deleted (ADR-008, open point
 * "Layers of Progress and Access control"; module rule 3). Projects' use cases call it. The Access
 * control module implements it, and decides only from the facts it is given: the person's role in
 * the project, and the project's state.
 */
public interface ProjectAccessPolicy {

	/**
	 * The decision for this action. The checks come in the order of ADR-004 decision 5: no role is
	 * {@link AccessDecision#NOT_FOUND}, then {@link AccessDecision#FORBIDDEN}, then
	 * {@link AccessDecision#CONFLICT}.
	 * @param action what the person asks to do
	 * @param state the project's state
	 * @param role the person's role, or empty if the person is not a member
	 */
	AccessDecision decide(ProjectAction action, ProjectState state, Optional<MemberRole> role);

}
