package com.matriz.accesscontrol.application;

import java.util.Optional;

import com.matriz.projects.AccessDecision;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectAccessPolicy;
import com.matriz.projects.ProjectAction;
import com.matriz.projects.ProjectState;

import org.springframework.stereotype.Component;

/**
 * Who may do what with a project, as the permission matrix proposes (requirements.md; ADR-004
 * decisions 5 and 6; Assumptions A-001, A-011):
 * <ul>
 * <li>A person who is not a member is told the project does not exist.</li>
 * <li>Any member may view it, also when it is Completed.</li>
 * <li>Only the Project Owner may edit or delete it, and a Project Participant is refused.</li>
 * <li>Nobody may edit or delete a Completed project.</li>
 * </ul>
 * It decides from the facts it is given and reads no data of other modules.
 */
@Component
class ProjectAccessRules implements ProjectAccessPolicy {

	@Override
	public AccessDecision decide(ProjectAction action, ProjectState state, Optional<MemberRole> role) {
		if (role.isEmpty()) {
			return AccessDecision.NOT_FOUND;
		}
		return switch (action) {
			case VIEW -> AccessDecision.ALLOWED;
			case EDIT, DELETE -> {
				if (role.get() != MemberRole.PROJECT_OWNER) {
					yield AccessDecision.FORBIDDEN;
				}
				yield state == ProjectState.ACTIVE ? AccessDecision.ALLOWED : AccessDecision.CONFLICT;
			}
		};
	}

}
