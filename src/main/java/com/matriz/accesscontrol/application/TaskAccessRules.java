package com.matriz.accesscontrol.application;

import java.util.Optional;

import com.matriz.projects.AccessDecision;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectState;
import com.matriz.tasks.TaskAccessPolicy;
import com.matriz.tasks.TaskAction;

import org.springframework.stereotype.Component;

/**
 * Who may do what with the tasks of a project, as the permission matrix proposes (requirements.md;
 * ADR-004 decisions 5 and 6; Assumptions A-001, A-011, A-019, A-021):
 * <ul>
 * <li>A person who is not a member is told the project, or the task, does not exist.</li>
 * <li>Any member may view the tasks, also when the project is Completed.</li>
 * <li>Until assignees exist (I7), only the Project Owner may create, edit, change the state of, or
 * delete a task; a Project Participant is refused.</li>
 * <li>Nobody may change a task of a Completed project.</li>
 * </ul>
 * It decides from the facts it is given and reads no data of other modules.
 */
@Component
class TaskAccessRules implements TaskAccessPolicy {

	@Override
	public AccessDecision decide(TaskAction action, ProjectState state, Optional<MemberRole> role) {
		if (role.isEmpty()) {
			return AccessDecision.NOT_FOUND;
		}
		if (action == TaskAction.VIEW) {
			return AccessDecision.ALLOWED;
		}
		if (role.get() != MemberRole.PROJECT_OWNER) {
			return AccessDecision.FORBIDDEN;
		}
		return state == ProjectState.ACTIVE ? AccessDecision.ALLOWED : AccessDecision.CONFLICT;
	}

}
