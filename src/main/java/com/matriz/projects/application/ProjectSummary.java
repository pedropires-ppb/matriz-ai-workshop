package com.matriz.projects.application;

import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectId;
import com.matriz.projects.ProjectState;

/**
 * A project as listed (openapi.yaml, ProjectSummary).
 * @param description {@code null} if the project has none
 */
public record ProjectSummary(ProjectId projectId, String name, String description, ProjectState state,
		MemberRole myRole) {

}
