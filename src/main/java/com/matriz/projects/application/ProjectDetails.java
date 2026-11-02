package com.matriz.projects.application;

import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectId;
import com.matriz.projects.ProjectState;
import com.matriz.projects.domain.Project;
import com.matriz.projects.domain.ProjectDescription;

/**
 * A project as shown to a member (openapi.yaml, Project).
 * @param description {@code null} if the project has none
 * @param myRole the role of the person who asked
 */
public record ProjectDetails(ProjectId projectId, String name, String description, ProjectState state,
		MemberRole myRole) {

	static ProjectDetails of(Project project, MemberRole role) {
		return new ProjectDetails(project.id(), project.name().value(),
				project.description().map(ProjectDescription::value).orElse(null), project.state(), role);
	}

}
