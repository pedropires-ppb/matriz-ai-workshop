package com.matriz.tasks.infrastructure;

import java.util.UUID;

import com.matriz.projects.ProjectId;
import com.matriz.tasks.TaskState;
import com.matriz.tasks.domain.TaskRepository.StateCount;

/** A row of the count query, before it becomes a {@link StateCount}. */
record StateCountRow(UUID project, TaskState state, long count) {

	StateCount toStateCount() {
		return new StateCount(new ProjectId(this.project), this.state, this.count);
	}

}
