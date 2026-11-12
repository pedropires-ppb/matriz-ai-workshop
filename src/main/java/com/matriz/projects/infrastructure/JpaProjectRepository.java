package com.matriz.projects.infrastructure;

import java.util.List;
import java.util.Optional;

import com.matriz.identity.PersonId;
import com.matriz.projects.ProjectId;
import com.matriz.projects.domain.Project;
import com.matriz.projects.domain.ProjectRepository;

import org.springframework.stereotype.Repository;

/** {@link ProjectRepository} with Spring Data JPA (ADR-008 decision 4). */
@Repository
class JpaProjectRepository implements ProjectRepository {

	private static final char ESCAPE = '!';

	private final SpringDataProjectRepository projects;

	JpaProjectRepository(SpringDataProjectRepository projects) {
		this.projects = projects;
	}

	@Override
	public Project save(Project project) {
		return this.projects.save(project);
	}

	@Override
	public Optional<Project> findById(ProjectId id) {
		return this.projects.findVisible(id);
	}

	@Override
	public Optional<Project> findByIdForUpdate(ProjectId id) {
		return this.projects.findVisibleForUpdate(id);
	}

	@Override
	public ProjectListingPage findAccessible(PersonId person, String nameContains, int page, int size) {
		String pattern = pattern(nameContains);
		long total = this.projects.countListings(person.value(), pattern);
		long offset = (long) (page - 1) * size;
		List<ListingRow> rows = offset >= total ? List.of()
				: this.projects.findListings(person.value(), pattern, offset, size);
		return new ProjectListingPage(rows.stream().map(ListingRow::toListing).toList(), total);
	}

	/** The text as a pattern that contains it, with none of its characters special; the query ignores case. */
	private static String pattern(String text) {
		if (text == null) {
			return "%";
		}
		StringBuilder pattern = new StringBuilder("%");
		for (char character : text.toCharArray()) {
			if (character == ESCAPE || character == '%' || character == '_') {
				pattern.append(ESCAPE);
			}
			pattern.append(character);
		}
		return pattern.append('%').toString();
	}

}
