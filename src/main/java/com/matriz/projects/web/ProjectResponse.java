package com.matriz.projects.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.matriz.projects.application.ProjectDetails;

/**
 * A project (openapi.yaml, Project). {@code description} is left out when the project has none;
 * {@code progress} is left out until I5.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ProjectResponse(String projectId, String name, String description, String state, String myRole) {

	static ProjectResponse of(ProjectDetails project) {
		return new ProjectResponse(project.projectId().toString(), project.name(), project.description(),
				project.state().name(), project.myRole().name());
	}

}
