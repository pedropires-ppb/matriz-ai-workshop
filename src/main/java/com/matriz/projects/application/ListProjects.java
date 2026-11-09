package com.matriz.projects.application;

import java.util.List;
import java.util.Map;

import com.matriz.identity.PersonId;
import com.matriz.projects.ProjectId;
import com.matriz.projects.ProjectProgress;
import com.matriz.projects.ProjectProgress.Figures;
import com.matriz.projects.domain.ProjectListing;
import com.matriz.projects.domain.Whitespace;
import com.matriz.projects.domain.ProjectRepository;
import com.matriz.projects.domain.ProjectRepository.ProjectListingPage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lists the projects of the person who asks, Active and Completed, never deleted ones (FR-001,
 * FR-022; US-007, US-023), a page at a time, by name (Assumptions A-024, A-032). Access control
 * here is the filter itself: only projects of which the person is a member are read. Each project
 * comes with its own task counts by state, from one query for the page (FR-024; US-025).
 */
@Service
public class ListProjects {

	private final ProjectRepository projects;

	private final ProjectProgress progress;

	ListProjects(ProjectRepository projects, ProjectProgress progress) {
		this.projects = projects;
		this.progress = progress;
	}

	/**
	 * A page of projects.
	 * @param nameContains the text to find in the names, ignoring letter case, or {@code null} for all
	 * @param page the number of the page, from 1
	 * @param size the maximum number of projects in the page
	 */
	@Transactional(readOnly = true)
	public ProjectsPage list(PersonId person, String nameContains, int page, int size) {
		String text = nameContains == null ? null : Whitespace.trim(nameContains);
		ProjectListingPage found = this.projects.findAccessible(person, text, page, size);
		Map<ProjectId, Figures> progress = this.progress
			.of(found.items().stream().map(ProjectListing::id).toList());
		List<ProjectSummary> items = found.items()
			.stream()
			.map((listing) -> summary(listing, progress.get(listing.id())))
			.toList();
		long totalPages = (found.totalItems() + size - 1) / size;
		return new ProjectsPage(items, page, size, found.totalItems(), totalPages);
	}

	private static ProjectSummary summary(ProjectListing listing, Figures progress) {
		return new ProjectSummary(listing.id(), listing.name().value(),
				listing.description() == null ? null : listing.description().value(), listing.state(), listing.role(),
				progress);
	}

}
