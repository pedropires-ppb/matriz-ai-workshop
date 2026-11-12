package com.matriz.tasks.application;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.matriz.projects.ProjectId;
import com.matriz.tasks.TaskCounting;
import com.matriz.tasks.TaskState;
import com.matriz.tasks.domain.TaskRepository;
import com.matriz.tasks.domain.TaskRepository.StateCount;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Gives Progress the counts of tasks by state, in the transaction of the use case that asks. */
@Component
class TaskCountsProvider implements TaskCounting {

	private final TaskRepository tasks;

	TaskCountsProvider(TaskRepository tasks) {
		this.tasks = tasks;
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
	public Map<ProjectId, TaskCounts> countByState(Collection<ProjectId> projects) {
		Map<ProjectId, Map<TaskState, Long>> byProject = new HashMap<>();
		for (ProjectId project : projects) {
			byProject.put(project, new EnumMap<>(TaskState.class));
		}
		if (!projects.isEmpty()) {
			for (StateCount row : this.tasks.countByState(projects)) {
				byProject.get(row.project()).put(row.state(), row.count());
			}
		}
		Map<ProjectId, TaskCounts> counts = new HashMap<>();
		byProject.forEach((project, states) -> counts.put(project,
				new TaskCounts(states.getOrDefault(TaskState.TO_DO, 0L),
						states.getOrDefault(TaskState.IN_PROGRESS, 0L), states.getOrDefault(TaskState.DONE, 0L))));
		return counts;
	}

}
