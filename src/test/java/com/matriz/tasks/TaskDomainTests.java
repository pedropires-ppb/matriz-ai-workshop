package com.matriz.tasks;

import java.time.Instant;
import java.util.UUID;

import com.matriz.identity.PersonId;
import com.matriz.projects.ProjectId;
import com.matriz.tasks.domain.Task;
import com.matriz.tasks.domain.TaskDescription;
import com.matriz.tasks.domain.TaskEdit;
import com.matriz.tasks.domain.TaskTitle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * The task's value objects and aggregate, without Spring or a database (Assumption A-033; ADR-008
 * decision 2).
 */
class TaskDomainTests {

	private static final ProjectId PROJECT = ProjectId.generate();

	@Test
	void aTitleIsTrimmedAndHasOneToOneHundredAndFiftyCharacters() {
		assertThat(new TaskTitle("  Write tests  ").value()).isEqualTo("Write tests");
		assertThat(new TaskTitle("a").value()).isEqualTo("a");
		assertThat(new TaskTitle("a".repeat(150)).value()).hasSize(150);
		assertThat(new TaskTitle("\uD83D\uDE00".repeat(150)).value())
			.hasSize(300)
			.satisfies((value) -> assertThat(value.codePointCount(0, value.length())).isEqualTo(150));
		assertThatIllegalArgumentException().isThrownBy(() -> new TaskTitle("a".repeat(151)))
			.withMessage("The title must have at most 150 characters.");
		assertThatIllegalArgumentException().isThrownBy(() -> new TaskTitle("\uD83D\uDE00".repeat(151)))
			.withMessage("The title must have at most 150 characters.");
		assertThat(new TaskTitle(" " + "a".repeat(150) + " ").value()).hasSize(150);
	}

	@ParameterizedTest
	@ValueSource(strings = { "\u0000", "\uD800", "\uDC00" })
	void aTitleThatCannotBeStoredIsRefused(String text) {
		assertThatIllegalArgumentException().isThrownBy(() -> new TaskTitle(text))
			.withMessage(TaskTitle.CANNOT_BE_STORED);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "\t", "\n ", "\u2003", "\u00A0", " \u00A0\u2007\u202F " })
	void aTitleThatIsEmptyAfterTrimmingIsRefused(String text) {
		assertThatIllegalArgumentException().isThrownBy(() -> new TaskTitle(text)).withMessage("A title is required.");
	}

	@Test
	void aMissingTitleIsRefused() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TaskTitle(null)).withMessage("A title is required.");
	}

	@Test
	void noBreakSpacesAreTrimmedLikeAnyOtherWhitespace() {
		assertThat(new TaskTitle("\u00A0Task\u00A0\u202F").value()).isEqualTo("Task");
		assertThat(TaskDescription.ofNullable("\u00A0\u00A0")).isNull();
		assertThat(TaskDescription.ofNullable("\u00A0About\u2007").value()).isEqualTo("About");
	}

	@Test
	void aDescriptionIsTrimmedAndHasAtMostTwoThousandCharacters() {
		assertThat(TaskDescription.ofNullable("  About  ").value()).isEqualTo("About");
		assertThat(TaskDescription.ofNullable("d".repeat(2000)).value()).hasSize(2000);
		assertThat(TaskDescription.ofNullable("\uD83D\uDE00".repeat(2000)).value()).hasSize(4000);
		assertThatIllegalArgumentException().isThrownBy(() -> TaskDescription.ofNullable("d".repeat(2001)))
			.withMessage("The description must have at most 2000 characters.");
		assertThatIllegalArgumentException().isThrownBy(() -> TaskDescription.ofNullable(" " + "d".repeat(2001)))
			.withMessage("The description must have at most 2000 characters.");
	}

	@Test
	void noDescriptionIsNullNotAnEmptyText() {
		assertThat(TaskDescription.ofNullable(null)).isNull();
		assertThat(TaskDescription.ofNullable("")).isNull();
		assertThat(TaskDescription.ofNullable(" \t ")).isNull();
	}

	@Test
	void aNewTaskIsToDoWithTheGivenPriorityAndNoAssignee() {
		Task task = task();

		assertThat(task.state()).isEqualTo(TaskState.TO_DO);
		assertThat(task.priority()).isEqualTo(TaskPriority.MEDIUM);
		assertThat(task.isDeleted()).isFalse();
		assertThat(task.assignees()).isEmpty();
		assertThat(task.description()).isEmpty();
		assertThat(task.projectId()).isEqualTo(PROJECT);
		assertThat(Task.create(TaskId.generate(), PROJECT, new TaskTitle("High"), null, TaskPriority.HIGH).priority())
			.isEqualTo(TaskPriority.HIGH);
	}

	@Test
	void anEditChangesOnlyWhatIsGivenAndNullRemovesTheDescription() {
		Task task = Task.create(TaskId.generate(), PROJECT, new TaskTitle("Old"), TaskDescription.ofNullable("Text"),
				TaskPriority.LOW);

		task.edit(new TaskEdit(new TaskTitle("New"), false, null, null));
		assertThat(task.title().value()).isEqualTo("New");
		assertThat(task.description().orElseThrow().value()).isEqualTo("Text");
		assertThat(task.priority()).isEqualTo(TaskPriority.LOW);

		task.edit(new TaskEdit(null, true, TaskDescription.ofNullable("Other"), TaskPriority.HIGH));
		assertThat(task.title().value()).isEqualTo("New");
		assertThat(task.description().orElseThrow().value()).isEqualTo("Other");
		assertThat(task.priority()).isEqualTo(TaskPriority.HIGH);

		task.edit(new TaskEdit(null, true, null, null));
		assertThat(task.description()).isEmpty();
	}

	@Test
	void anyStateCanBeSetFromAnyState() {
		Task task = task();
		for (TaskState from : TaskState.values()) {
			for (TaskState to : TaskState.values()) {
				task.changeState(from);
				task.changeState(to);
				assertThat(task.state()).isEqualTo(to);
			}
		}
	}

	@Test
	void aDeletedTaskIsHiddenAndRefusesEveryChange() {
		Task task = task();

		task.delete(Instant.now());

		assertThat(task.isDeleted()).isTrue();
		assertThatIllegalStateException().isThrownBy(() -> task.changeState(TaskState.DONE));
		assertThatIllegalStateException().isThrownBy(() -> task.edit(new TaskEdit(null, true, null, null)));
		assertThatIllegalStateException().isThrownBy(() -> task.delete(Instant.now()));
	}

	@Test
	void severalPeopleCanBeAssignedAndEachOneRemovedWithoutChangingTheState() {
		Task task = task();
		PersonId ana = new PersonId(UUID.randomUUID());
		PersonId rui = new PersonId(UUID.randomUUID());

		task.assign(ana);
		task.assign(rui);
		assertThat(task.assignees()).containsExactlyInAnyOrder(ana, rui);
		assertThat(task.state()).isEqualTo(TaskState.TO_DO);

		task.unassign(ana);
		assertThat(task.assignees()).containsExactly(rui);
		task.unassign(rui);
		assertThat(task.assignees()).isEmpty();
		assertThat(task.state()).isEqualTo(TaskState.TO_DO);
	}

	@Test
	void aRepeatedAssignmentChangesNothingAndUnassigningANonAssigneeIsANoOp() {
		Task task = task();
		PersonId ana = new PersonId(UUID.randomUUID());

		task.assign(ana);
		task.assign(ana);
		task.unassign(new PersonId(UUID.randomUUID()));

		assertThat(task.assignees()).containsExactly(ana);
	}

	@Test
	void aDeletedTaskRefusesAssignmentChangesAndLosesItsAssignees() {
		Task task = task();
		PersonId ana = new PersonId(UUID.randomUUID());
		task.assign(ana);

		task.delete(Instant.now());

		assertThat(task.assignees()).isEmpty();
		assertThatIllegalStateException().isThrownBy(() -> task.assign(ana));
		assertThatIllegalStateException().isThrownBy(() -> task.unassign(ana));
	}

	private static Task task() {
		return Task.create(TaskId.generate(), PROJECT, new TaskTitle("Task"), null, TaskPriority.MEDIUM);
	}

}
