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
	COMPLETE

}
