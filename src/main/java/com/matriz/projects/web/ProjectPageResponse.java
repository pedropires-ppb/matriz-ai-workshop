package com.matriz.projects.web;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.matriz.projects.application.ProjectSummary;
import com.matriz.projects.application.ProjectsPage;

/** One page of My projects (openapi.yaml, ProjectPage). */
record ProjectPageResponse(List<Item> items, int page, int size, long totalItems, long totalPages) {

	static ProjectPageResponse of(ProjectsPage page) {
		return new ProjectPageResponse(page.items().stream().map(Item::of).toList(), page.page(), page.size(),
				page.totalItems(), page.totalPages());
	}

	/**
	 * A project in the list (openapi.yaml, ProjectSummary). {@code description} is left out when the
	 * project has none; {@code taskCounts} is left out until I5.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	record Item(String projectId, String name, String description, String state, String myRole) {

		static Item of(ProjectSummary project) {
			return new Item(project.projectId().toString(), project.name(), project.description(),
					project.state().name(), project.myRole().name());
		}

	}

}
