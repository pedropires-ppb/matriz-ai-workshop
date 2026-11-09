package com.matriz.progress;

import com.matriz.progress.domain.Progress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** The progress read model, without Spring or a database (FR-012; EX-005, EX-027; Assumption A-026). */
class ProgressDomainTests {

	@ParameterizedTest(name = "{0} Done of {1} is {2}%")
	@CsvSource({ "1, 3, 33", "2, 3, 67", "1, 8, 13", "1, 2, 50", "2, 4, 50", "0, 5, 0", "5, 5, 100", "1, 200, 1",
			"199, 200, 100", "1, 201, 0", "3, 8, 38" })
	void thePercentageDoneIsRoundedToAWholeNumberWithHalvesUp(long done, long total, int percent) {
		assertThat(new Progress(total - done, 0, done).percentDone()).hasValue(percent);
	}

	@Test
	void inProgressTasksCountInTheTotalButNotAsDone() {
		Progress progress = new Progress(1, 1, 2);

		assertThat(progress.total()).isEqualTo(4);
		assertThat(progress.percentDone()).hasValue(50);
	}

	@Test
	void aProjectWithNoTasksHasNoPercentage() {
		Progress progress = new Progress(0, 0, 0);

		assertThat(progress.total()).isZero();
		assertThat(progress.percentDone()).isEmpty();
	}

	@Test
	void aCountCannotBeNegative() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Progress(-1, 0, 0));
		assertThatIllegalArgumentException().isThrownBy(() -> new Progress(0, -1, 0));
		assertThatIllegalArgumentException().isThrownBy(() -> new Progress(0, 0, -1));
	}

}
