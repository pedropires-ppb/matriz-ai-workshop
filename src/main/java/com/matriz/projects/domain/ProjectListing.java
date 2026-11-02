package com.matriz.projects.domain;

import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectId;
import com.matriz.projects.ProjectState;

/**
 * One project as it appears in a list, with the role of the person who lists (openapi.yaml,
 * ProjectSummary).
 * @param description {@code null} if the project has none
 */
public record ProjectListing(ProjectId id, ProjectName name, ProjectDescription description, ProjectState state,
		MemberRole role) {

}
