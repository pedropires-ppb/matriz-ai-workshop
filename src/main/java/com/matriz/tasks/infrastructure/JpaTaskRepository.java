package com.matriz.tasks.infrastructure;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.matriz.identity.PersonId;
import com.matriz.projects.ProjectId;
import com.matriz.tasks.TaskId;
import com.matriz.tasks.TaskPriority;
import com.matriz.tasks.TaskState;
import com.matriz.tasks.domain.Task;
import com.matriz.tasks.domain.TaskRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

/** {@link TaskRepository} with Spring Data JPA (ADR-008 decision 4). */
@Repository
class JpaTaskRepository implements TaskRepository {

	private final SpringDataTaskRepository tasks;

	JpaTaskRepository(SpringDataTaskRepository tasks) {
		this.tasks = tasks;
	}

	@Override
	public Task save(Task task) {
		return this.tasks.save(task);
	}

	@Override
	public Optional<Task> findById(ProjectId project, TaskId id) {
		return this.tasks.findVisible(project.value(), id.value());
	}

	@Override
	public Set<PersonId> assignees(ProjectId project) {
		return this.tasks.assigneesOf(project.value()).stream().map(PersonId::new).collect(Collectors.toSet());
	}

	@Override
	public boolean hasUnfinished(ProjectId project) {
		return this.tasks.hasStateOtherThan(project.value(), TaskState.DONE);
	}

	@Override
	public List<StateCount> countByState(Collection<ProjectId> projects) {
		return this.tasks.countByState(projects.stream().map(ProjectId::value).toList())
			.stream()
			.map(StateCountRow::toStateCount)
			.toList();
	}

	@Override
	public TaskPage find(ProjectId project, Set<TaskState> states, Set<TaskPriority> priorities, int page,
			int size) {
		Page<Task> found = this.tasks.findPage(project.value(),
				states.isEmpty() ? EnumSet.allOf(TaskState.class) : states,
				priorities.isEmpty() ? EnumSet.allOf(TaskPriority.class) : priorities, PageRequest.of(page - 1, size));
		return new TaskPage(found.getContent(), found.getTotalElements());
	}

}
