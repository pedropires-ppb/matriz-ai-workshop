package com.matriz.projects.infrastructure;

import java.util.UUID;

import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectId;
import com.matriz.projects.ProjectState;
import com.matriz.projects.domain.ProjectDescription;
import com.matriz.projects.domain.ProjectListing;
import com.matriz.projects.domain.ProjectName;

/** A row of the list query, before it becomes a {@link ProjectListing}. */
record ListingRow(UUID id, String name, String description, ProjectState state, MemberRole role) {

	ProjectListing toListing() {
		return new ProjectListing(new ProjectId(this.id), new ProjectName(this.name),
				this.description == null ? null : new ProjectDescription(this.description), this.state, this.role);
	}

}
