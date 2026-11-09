package com.matriz.accesscontrol.application;

import java.util.List;
import java.util.Optional;

import com.matriz.projects.AccessDecision;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectState;
import com.matriz.tasks.TaskAction;
import com.matriz.tasks.TaskFacts;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who may do what with the tasks of a project (ADR-004 decisions 5 and 6; Assumptions A-001, A-019,
 * A-020, A-021, A-035), decided from facts alone, so it runs without Spring or a
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
	void aProjectParticipantCannotCreateEditOrDeleteATask() {
		for (TaskAction action : List.of(TaskAction.CREATE, TaskAction.EDIT, TaskAction.DELETE)) {
			for (ProjectState state : ProjectState.values()) {
				assertThat(this.rules.decide(action, state, Optional.of(MemberRole.PROJECT_PARTICIPANT),
						new TaskFacts(true, true)))
					.isEqualTo(AccessDecision.FORBIDDEN);
			}
		}
	}

	@Test
	void aProjectParticipantChangesTheStateOnlyOfATaskTheyAreAssignedTo() {
		Optional<MemberRole> participant = Optional.of(MemberRole.PROJECT_PARTICIPANT);

		assertThat(this.rules.decide(TaskAction.CHANGE_STATE, ProjectState.ACTIVE, participant, TaskFacts.NONE))
			.isEqualTo(AccessDecision.FORBIDDEN);
		assertThat(this.rules.decide(TaskAction.CHANGE_STATE, ProjectState.ACTIVE, participant,
				new TaskFacts(true, false)))
			.isEqualTo(AccessDecision.ALLOWED);
		assertThat(this.rules.decide(TaskAction.CHANGE_STATE, ProjectState.COMPLETED, participant,
				new TaskFacts(true, false)))
			.isEqualTo(AccessDecision.CONFLICT);
		assertThat(this.rules.decide(TaskAction.CHANGE_STATE, ProjectState.COMPLETED, participant, TaskFacts.NONE))
			.isEqualTo(AccessDecision.FORBIDDEN);
	}

	@Test
	void aProjectParticipantAssignsOrUnassignsOnlyThemselves() {
		Optional<MemberRole> participant = Optional.of(MemberRole.PROJECT_PARTICIPANT);
		for (TaskAction action : List.of(TaskAction.ASSIGN, TaskAction.UNASSIGN)) {
			assertThat(this.rules.decide(action, ProjectState.ACTIVE, participant, TaskFacts.NONE))
				.isEqualTo(AccessDecision.FORBIDDEN);
			assertThat(this.rules.decide(action, ProjectState.ACTIVE, participant, new TaskFacts(true, false)))
				.isEqualTo(AccessDecision.FORBIDDEN);
			assertThat(this.rules.decide(action, ProjectState.ACTIVE, participant, new TaskFacts(false, true)))
				.isEqualTo(AccessDecision.ALLOWED);
			assertThat(this.rules.decide(action, ProjectState.COMPLETED, participant, new TaskFacts(false, true)))
				.isEqualTo(AccessDecision.CONFLICT);
		}
	}

	@Test
	void theProjectOwnerChangesTasksOnlyWhileTheProjectIsActive() {
		for (TaskAction action : TaskAction.values()) {
			if (action == TaskAction.VIEW || action == TaskAction.COMMENT) {
				continue;
			}
			assertThat(this.rules.decide(action, ProjectState.ACTIVE, Optional.of(MemberRole.PROJECT_OWNER)))
				.isEqualTo(AccessDecision.ALLOWED);
			assertThat(this.rules.decide(action, ProjectState.COMPLETED, Optional.of(MemberRole.PROJECT_OWNER)))
				.isEqualTo(AccessDecision.CONFLICT);
		}
	}

	@Test
	void everyMemberCanCommentWhateverTheStateOfTheProject() {
		for (MemberRole role : MemberRole.values()) {
			for (ProjectState state : ProjectState.values()) {
				assertThat(this.rules.decide(TaskAction.COMMENT, state, Optional.of(role)))
					.isEqualTo(AccessDecision.ALLOWED);
				assertThat(this.rules.decide(TaskAction.COMMENT, state, Optional.of(role), TaskFacts.NONE))
					.isEqualTo(AccessDecision.ALLOWED);
			}
		}
	}

}
