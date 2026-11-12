package com.matriz.progress.domain;

import java.util.OptionalInt;

/**
 * A project's progress: the number of its tasks, not deleted, in each state, and the percentage Done
 * derived from them (FR-012; glossary, Progress; Assumptions A-006, A-026).
 */
public record Progress(long toDo, long inProgress, long done) {

	public Progress {
		if (toDo < 0 || inProgress < 0 || done < 0) {
			throw new IllegalArgumentException("A count of tasks cannot be negative.");
		}
	}

	public long total() {
		return this.toDo + this.inProgress + this.done;
	}

	/**
	 * Done tasks / all tasks × 100, rounded to the nearest whole number, halves up: 1 of 3 is 33, 2 of
	 * 3 is 67, and 1 of 8 is 13 (EX-027; Assumption A-026). Computed in whole numbers, so that no
	 * floating-point error moves a half.
	 * @return empty when there are no tasks; the User interface then shows "No tasks" (EX-005)
	 */
	public OptionalInt percentDone() {
		long total = total();
		if (total == 0) {
			return OptionalInt.empty();
		}
		return OptionalInt.of((int) ((200 * this.done + total) / (2 * total)));
	}

}
