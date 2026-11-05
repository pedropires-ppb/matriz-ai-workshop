package com.matriz.projects;

import java.time.Instant;
import java.util.UUID;

import com.matriz.identity.PersonId;
import com.matriz.projects.domain.Project;
import com.matriz.projects.domain.ProjectDescription;
import com.matriz.projects.domain.ProjectEdit;
import com.matriz.projects.domain.ProjectName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * The project's value objects and aggregate, without Spring or a database (Assumption A-031;
 * ADR-008 decision 2).
 */
class ProjectDomainTests {

	private static final PersonId OWNER = new PersonId(UUID.randomUUID());

	private static final PersonId OTHER = new PersonId(UUID.randomUUID());

	@Test
	void aNameIsTrimmedAndHasOneToOneHundredCharacters() {
		assertThat(new ProjectName("  Matriz  ").value()).isEqualTo("Matriz");
		assertThat(new ProjectName("a").value()).isEqualTo("a");
		assertThat(new ProjectName("a".repeat(100)).value()).hasSize(100);
		assertThat(new ProjectName("\uD83D\uDE00".repeat(100)).value()).hasSize(200);
		assertThatIllegalArgumentException().isThrownBy(() -> new ProjectName("a".repeat(101)))
			.withMessage("The name must have at most 100 characters.");
		assertThatIllegalArgumentException().isThrownBy(() -> new ProjectName("\uD83D\uDE00".repeat(101)))
			.withMessage("The name must have at most 100 characters.");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "\t", "\n ", "\u2003", "\u00A0", " \u00A0\u2007\u202F " })
	void aNameThatIsEmptyAfterTrimmingIsRefused(String text) {
		assertThatIllegalArgumentException().isThrownBy(() -> new ProjectName(text))
			.withMessage("A name is required.");
	}

	@Test
	void noBreakSpacesAreTrimmedLikeAnyOtherWhitespace() {
		assertThat(new ProjectName("\u00A0Matriz\u00A0\u202F").value()).isEqualTo("Matriz");
		assertThat(ProjectDescription.ofNullable("\u00A0\u00A0")).isNull();
		assertThat(ProjectDescription.ofNullable("\u00A0About\u2007").value()).isEqualTo("About");
	}

	@Test
	void nameLengthLimitCountsTheTrimmedText() {
		assertThat(new ProjectName(" " + "a".repeat(100) + " ").value()).hasSize(100);
	}

	@Test
	void aDescriptionIsTrimmedAndHasAtMostOneThousandCharacters() {
		assertThat(ProjectDescription.ofNullable("  About  ").value()).isEqualTo("About");
		assertThat(ProjectDescription.ofNullable("d".repeat(1000)).value()).hasSize(1000);
		assertThatIllegalArgumentException().isThrownBy(() -> ProjectDescription.ofNullable("d".repeat(1001)))
			.withMessage("The description must have at most 1000 characters.");
		assertThatIllegalArgumentException().isThrownBy(() -> ProjectDescription.ofNullable(" " + "d".repeat(1001)))
			.withMessage("The description must have at most 1000 characters.");
	}

	@Test
	void noDescriptionIsNullNotAnEmptyText() {
		assertThat(ProjectDescription.ofNullable(null)).isNull();
		assertThat(ProjectDescription.ofNullable("")).isNull();
		assertThat(ProjectDescription.ofNullable(" \t ")).isNull();
	}

	@Test
	void aNewProjectIsActiveAndItsCreatorIsItsOnlyMemberAndProjectOwner() {
		Project project = project();

		assertThat(project.state()).isEqualTo(ProjectState.ACTIVE);
		assertThat(project.isDeleted()).isFalse();
		assertThat(project.roleOf(OWNER)).contains(MemberRole.PROJECT_OWNER);
		assertThat(project.roleOf(OTHER)).isEmpty();
		assertThat(project.description()).isEmpty();
	}

	@Test
	void theOwnerEditsTheNameAndTheDescriptionAndNullRemovesIt() {
		Project project = project();

		project.edit(OWNER, new ProjectEdit(new ProjectName("Changed"), true, ProjectDescription.ofNullable("Text")));
		assertThat(project.name().value()).isEqualTo("Changed");
		assertThat(project.description().orElseThrow().value()).isEqualTo("Text");

		project.edit(OWNER, new ProjectEdit(null, false, null));
		assertThat(project.name().value()).isEqualTo("Changed");
		assertThat(project.description()).isPresent();

		project.edit(OWNER, new ProjectEdit(null, true, null));
		assertThat(project.description()).isEmpty();
	}

	@Test
	void onlyTheProjectOwnerCanEditOrDelete() {
		Project project = project();

		assertThatIllegalStateException().isThrownBy(() -> project.edit(OTHER, new ProjectEdit(null, false, null)));
		assertThatIllegalStateException().isThrownBy(() -> project.delete(OTHER, Instant.now()));
		assertThat(project.isDeleted()).isFalse();
	}

	@Test
	void aDeletedProjectCannotBeEditedOrDeletedAgain() {
		Project project = project();
		project.delete(OWNER, Instant.now());

		assertThat(project.isDeleted()).isTrue();
		assertThatIllegalStateException().isThrownBy(() -> project.edit(OWNER, new ProjectEdit(null, false, null)));
		assertThatIllegalStateException().isThrownBy(() -> project.delete(OWNER, Instant.now()));
	}

	private static Project project() {
		return Project.create(ProjectId.generate(), new ProjectName("Matriz"), null, OWNER);
	}

}
