package com.matriz.projects;

/** What a person asks to do with a project, for the access policy to decide. */
public enum ProjectAction {

	/** Read the project: view it, or see it in a list. */
	VIEW,

	/** Change its name or description. */
	EDIT,

	/** Delete it. */
	DELETE,

	/** Complete it (FR-007). */
	COMPLETE,

	/** Add or remove a Project Participant (FR-004, FR-005). */
	MANAGE_MEMBERS,

	/**
	 * See the people who can be added (Assumption A-030): the Project Owner only, also while the
	 * project is Completed.
	 */
	LIST_ELIGIBLE_PARTICIPANTS

}
