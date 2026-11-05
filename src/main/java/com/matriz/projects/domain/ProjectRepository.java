package com.matriz.projects.domain;

import java.util.List;
import java.util.Optional;

import com.matriz.identity.PersonId;
import com.matriz.projects.ProjectId;

/**
 * The projects (ADR-008 decision 4). Every read path hides deleted projects (Assumption A-022);
 * the lists show only the projects of the person who asks.
 */
public interface ProjectRepository {

	Project save(Project project);

	/** The project, unless it does not exist or is deleted. */
	Optional<Project> findById(ProjectId id);

	/**
	 * Like {@link #findById}, and locks the project's row until the end of the transaction, so that
	 * a change to it waits for any other change (ADR-006).
	 */
	Optional<Project> findByIdForUpdate(ProjectId id);

	/**
	 * A page of the projects in which the person is a member, not deleted, whose name contains the
	 * text, ignoring letter case, ordered by name ignoring letter case and then by identifier
	 * (Assumptions A-024 and A-032).
	 * @param nameContains the text to find in the names, or {@code null} for all projects; it is
	 * not a pattern
	 * @param page the number of the page, from 1
	 * @param size the maximum number of projects in the page
	 */
	ProjectListingPage findAccessible(PersonId person, String nameContains, int page, int size);

	/** A page of listings and the number of all the projects that match. */
	record ProjectListingPage(List<ProjectListing> items, long totalItems) {

	}

}
