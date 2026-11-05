package com.matriz.projects;

import java.util.Optional;

import com.matriz.identity.PersonId;

/**
 * What other modules may learn about a project: its state and a person's role in it (ADR-008, module
 * rule 3). Tasks' use cases call it, inside their own transaction, so that a change to a task takes
 * the project's lock (ADR-006) and the access policy decides from the project's state and the
 * person's role. A project that does not exist, or is deleted, is not found (Assumption A-022).
 */
public interface ProjectAccess {

	/** The facts about the project, or empty if there is no such project. */
	Optional<ProjectFacts> find(ProjectId project, PersonId person);

	/**
	 * Like {@link #find}, and locks the project's row until the end of the transaction, so that a
	 * change to the project or to one of its tasks waits for any other change (ADR-006).
	 */
	Optional<ProjectFacts> findForUpdate(ProjectId project, PersonId person);

	/**
	 * The facts the access policy needs.
	 * @param role the person's role, or empty if the person is not a member
	 */
	record ProjectFacts(ProjectState state, Optional<MemberRole> role) {

	}

}
