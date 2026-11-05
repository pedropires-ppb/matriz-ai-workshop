package com.matriz.accesscontrol.application;

import java.util.Optional;

import com.matriz.projects.AccessDecision;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectState;
import com.matriz.tasks.TaskAction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who may do what with the tasks of a project, before assignees exist (ADR-004 decisions 5 and 6;
 * Assumptions A-001, A-019, A-021), decided from facts alone, so it runs without Spring or a
 * database.
 */
class TaskAccessRulesTests {

	private final TaskAccessRules rules = new TaskAccessRules();

	@Test
	void aPersonWhoIsNotAMemberIsToldTheProjectDoesNotExistForEveryAction() {
		for (TaskAction action : TaskAction.values()) {
			for (ProjectState state : ProjectState.values()) {
				assertThat(this.rules.decide(action, state, Optional.empty())).isEqualTo(AccessDecision.NOT_FOUND);
			}
		}
	}

	@Test
	void everyMemberCanViewTheTasksOfAnActiveOrCompletedProject() {
		for (MemberRole role : MemberRole.values()) {
			for (ProjectState state : ProjectState.values()) {
				assertThat(this.rules.decide(TaskAction.VIEW, state, Optional.of(role)))
					.isEqualTo(AccessDecision.ALLOWED);
			}
		}
	}

	@Test
	void aProjectParticipantCannotChangeATaskInAnyWay() {
		for (TaskAction action : TaskAction.values()) {
			if (action == TaskAction.VIEW) {
				continue;
			}
			for (ProjectState state : ProjectState.values()) {
				assertThat(this.rules.decide(action, state, Optional.of(MemberRole.PROJECT_PARTICIPANT)))
					.isEqualTo(AccessDecision.FORBIDDEN);
			}
		}
	}

	@Test
	void theProjectOwnerChangesTasksOnlyWhileTheProjectIsActive() {
		for (TaskAction action : TaskAction.values()) {
			if (action == TaskAction.VIEW) {
				continue;
			}
			assertThat(this.rules.decide(action, ProjectState.ACTIVE, Optional.of(MemberRole.PROJECT_OWNER)))
				.isEqualTo(AccessDecision.ALLOWED);
			assertThat(this.rules.decide(action, ProjectState.COMPLETED, Optional.of(MemberRole.PROJECT_OWNER)))
				.isEqualTo(AccessDecision.CONFLICT);
		}
	}

}
