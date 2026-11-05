package com.matriz.tasks.application;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.matriz.identity.PersonDirectory;
import com.matriz.identity.PersonId;
import com.matriz.tasks.application.TaskDetails.Assignee;
import com.matriz.tasks.domain.Task;
import com.matriz.tasks.domain.TaskDescription;

import org.springframework.stereotype.Component;

/**
 * Makes the details of tasks, with the display names of their assignees, which Identity gives
 * (openapi.yaml, Task). A person who is no longer known is left out. Call it inside the use case's
 * transaction, which keeps the assignments readable.
 */
@Component
class TaskViews {

	private static final Comparator<Assignee> BY_NAME = Comparator.comparing(Assignee::displayName)
		.thenComparing((assignee) -> assignee.personId().value());

	private final PersonDirectory people;

	TaskViews(PersonDirectory people) {
		this.people = people;
	}

	TaskDetails of(Task task) {
		return of(List.of(task)).get(0);
	}

	List<TaskDetails> of(Collection<Task> tasks) {
		Set<PersonId> everyone = new HashSet<>();
		tasks.forEach((task) -> everyone.addAll(task.assignees()));
		Map<PersonId, String> names = this.people.displayNames(everyone);
		return tasks.stream().map((task) -> details(task, names)).toList();
	}

	private static TaskDetails details(Task task, Map<PersonId, String> names) {
		List<Assignee> assignees = task.assignees()
			.stream()
			.filter(names::containsKey)
			.map((person) -> new Assignee(person, names.get(person)))
			.sorted(BY_NAME)
			.toList();
		return new TaskDetails(task.id(), task.projectId(), task.title().value(),
				task.description().map(TaskDescription::value).orElse(null), task.state(), task.priority(),
				assignees);
	}

}
