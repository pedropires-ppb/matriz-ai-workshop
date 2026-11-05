package com.matriz.tasks;

/** What a person asks to do with the tasks of a project, for the access policy to decide. */
public enum TaskAction {

	/** Read the tasks: list or view them. */
	VIEW,

	/** Create a task. */
	CREATE,

	/** Change a task's title, description, or priority. */
	EDIT,

	/** Change a task's state. */
	CHANGE_STATE,

	/** Assign a member to a task. */
	ASSIGN,

	/** Remove an assignee from a task. */
	UNASSIGN,

	/** Delete a task. */
	DELETE,

	/** Add a comment to a task: allowed to every member, in any state of the project (rule 5). */
	COMMENT

}
