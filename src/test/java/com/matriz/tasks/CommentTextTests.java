package com.matriz.tasks;

import com.matriz.tasks.domain.CommentText;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** A comment's text, without Spring or a database (Assumptions A-033, A-036; ADR-008 decision 2). */
class CommentTextTests {

	@Test
	void aTextIsTrimmedAndHasOneToTwoThousandCharacters() {
		assertThat(new CommentText("  Draft sent  ").value()).isEqualTo("Draft sent");
		assertThat(new CommentText("a").value()).isEqualTo("a");
		assertThat(new CommentText("a".repeat(2000)).value()).hasSize(2000);
		assertThat(new CommentText("\uD83D\uDE00".repeat(2000)).value()).hasSize(4000);
		assertThat(new CommentText(" " + "a".repeat(2000) + " ").value()).hasSize(2000);
		assertThatIllegalArgumentException().isThrownBy(() -> new CommentText("a".repeat(2001)))
			.withMessage("The text must have at most 2000 characters.");
		assertThatIllegalArgumentException().isThrownBy(() -> new CommentText("\uD83D\uDE00".repeat(2001)))
			.withMessage("The text must have at most 2000 characters.");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "\t", "\n ", "\u2003", "\u00A0", " \u00A0\u2007\u202F " })
	void aTextThatIsEmptyAfterTrimmingIsRefused(String text) {
		assertThatIllegalArgumentException().isThrownBy(() -> new CommentText(text))
			.withMessage("The text is required.");
	}

	@Test
	void aMissingTextIsRefused() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CommentText(null))
			.withMessage("The text is required.");
	}

	@Test
	void noBreakSpacesAreTrimmedLikeAnyOtherWhitespace() {
		assertThat(new CommentText("\u00A0Done\u00A0\u202F").value()).isEqualTo("Done");
	}

	@Test
	void whitespaceInsideTheTextIsKept() {
		assertThat(new CommentText("one\n\ntwo  three").value()).isEqualTo("one\n\ntwo  three");
	}

}
