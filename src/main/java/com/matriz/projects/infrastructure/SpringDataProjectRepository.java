package com.matriz.projects.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.matriz.projects.ProjectId;
import com.matriz.projects.domain.Project;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA's side of {@link JpaProjectRepository} (ADR-008 decision 4). Every query leaves
 * out deleted projects (Assumption A-022).
 */
interface SpringDataProjectRepository extends Repository<Project, ProjectId> {

	Project save(Project project);

	@Query("select p from Project p where p.id = :id and p.deletedAt is null")
	Optional<Project> findVisible(@Param("id") ProjectId id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Project p where p.id = :id and p.deletedAt is null")
	Optional<Project> findVisibleForUpdate(@Param("id") ProjectId id);

	/**
	 * The projects of one person whose name matches the pattern, which the caller has escaped with
	 * {@code !}; both sides are lower-cased by the database.
	 */
	@Query(value = """
			select new com.matriz.projects.infrastructure.ListingRow(p.id.value, p.name.value, p.description.value,
					p.state, m.role)
			from Project p join p.memberships m
			where m.id.personId.value = :person and p.deletedAt is null
				and lower(p.name.value) like lower(:pattern) escape '!'
			order by lower(p.name.value), p.id.value
			""", countQuery = """
			select count(p) from Project p join p.memberships m
			where m.id.personId.value = :person and p.deletedAt is null
				and lower(p.name.value) like lower(:pattern) escape '!'
			""")
	Page<ListingRow> findListings(@Param("person") UUID person, @Param("pattern") String pattern, Pageable pageable);

}
