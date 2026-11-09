package com.matriz.projects;

/**
 * What Projects needs to know about a project's tasks, which Tasks owns: Projects defines this
 * interface, and Tasks implements it, so that Projects does not depend on Tasks (ADR-008 decision 3;
 * architecture module rule 2). Projects' use cases call it inside their own transaction, after
 * locking the project, so that no task changes between the check and the change (ADR-006).
 */
public interface ProjectTasks {

	/**
	 * Whether any task of the project is To do or In progress. Deleted tasks are not counted
	 * (EX-018; Assumptions A-007, A-022).
	 */
	boolean hasUnfinishedTasks(ProjectId project);

}
