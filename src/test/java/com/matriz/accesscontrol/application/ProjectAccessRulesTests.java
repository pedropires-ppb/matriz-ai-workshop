package com.matriz.accesscontrol.application;

import java.util.Optional;

import com.matriz.projects.AccessDecision;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectAction;
import com.matriz.projects.ProjectState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who may do what with a project (ADR-004 decisions 5 and 6; Assumptions A-001, A-011), decided
 * from facts alone, so it runs without Spring or a database.
 */
class ProjectAccessRulesTests {

	private final ProjectAccessRules rules = new ProjectAccessRules();

	@Test
	void aPersonWhoIsNotAMemberIsToldTheProjectDoesNotExistForEveryAction() {
		for (ProjectAction action : ProjectAction.values()) {
			for (ProjectState state : ProjectState.values()) {
				assertThat(this.rules.decide(action, state, Optional.empty())).isEqualTo(AccessDecision.NOT_FOUND);
			}
		}
	}

	@Test
	void everyMemberCanViewAnActiveOrCompletedProject() {
		for (MemberRole role : MemberRole.values()) {
			for (ProjectState state : ProjectState.values()) {
				assertThat(this.rules.decide(ProjectAction.VIEW, state, Optional.of(role)))
					.isEqualTo(AccessDecision.ALLOWED);
			}
		}
	}

	@Test
	void aProjectParticipantCannotEditDeleteOrCompleteEvenWhenCompleted() {
		for (ProjectAction action : new ProjectAction[] { ProjectAction.EDIT, ProjectAction.DELETE, ProjectAction.COMPLETE }) {
			for (ProjectState state : ProjectState.values()) {
				assertThat(this.rules.decide(action, state, Optional.of(MemberRole.PROJECT_PARTICIPANT)))
					.isEqualTo(AccessDecision.FORBIDDEN);
			}
		}
	}

	@Test
	void theProjectOwnerEditsDeletesAndCompletesOnlyWhileTheProjectIsActive() {
		for (ProjectAction action : new ProjectAction[] { ProjectAction.EDIT, ProjectAction.DELETE, ProjectAction.COMPLETE }) {
			assertThat(this.rules.decide(action, ProjectState.ACTIVE, Optional.of(MemberRole.PROJECT_OWNER)))
				.isEqualTo(AccessDecision.ALLOWED);
			assertThat(this.rules.decide(action, ProjectState.COMPLETED, Optional.of(MemberRole.PROJECT_OWNER)))
				.isEqualTo(AccessDecision.CONFLICT);
		}
	}

}
