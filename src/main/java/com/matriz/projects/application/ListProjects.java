package com.matriz.projects.application;

import java.util.List;

import com.matriz.identity.PersonId;
import com.matriz.projects.domain.ProjectListing;
import com.matriz.projects.domain.ProjectRepository;
import com.matriz.projects.domain.ProjectRepository.ProjectListingPage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lists the projects of the person who asks, Active and Completed, never deleted ones (FR-001,
 * FR-022; US-007, US-023), a page at a time, by name (Assumptions A-024, A-032). Access control
 * here is the filter itself: only projects of which the person is a member are read.
 */
@Service
public class ListProjects {

	private final ProjectRepository projects;

	ListProjects(ProjectRepository projects) {
		this.projects = projects;
	}

	/**
	 * A page of projects.
	 * @param nameContains the text to find in the names, ignoring letter case, or {@code null} for all
	 * @param page the number of the page, from 1
	 * @param size the maximum number of projects in the page
	 */
	@Transactional(readOnly = true)
	public ProjectsPage list(PersonId person, String nameContains, int page, int size) {
		String text = nameContains == null ? null : nameContains.strip();
		ProjectListingPage found = this.projects.findAccessible(person, text, page, size);
		List<ProjectSummary> items = found.items().stream().map(ListProjects::summary).toList();
		long totalPages = (found.totalItems() + size - 1) / size;
		return new ProjectsPage(items, page, size, found.totalItems(), totalPages);
	}

	private static ProjectSummary summary(ProjectListing listing) {
		return new ProjectSummary(listing.id(), listing.name().value(),
				listing.description() == null ? null : listing.description().value(), listing.state(), listing.role());
	}

}
