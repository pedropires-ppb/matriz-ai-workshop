package com.matriz.projects.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.matriz.projects.ProjectId;
import com.matriz.projects.domain.Project;

import jakarta.persistence.LockModeType;

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
			select p.project_id, p.name, p.description, p.state, m.role
			from project p join membership m on m.project_id = p.project_id
			where m.person_id = :person and p.deleted_at is null
				and lower(p.name) like lower(:pattern) escape '!'
			order by lower(p.name), p.project_id
			limit :size offset :offset
			""", nativeQuery = true)
	List<ListingRow> findListings(@Param("person") UUID person, @Param("pattern") String pattern,
			@Param("offset") long offset, @Param("size") int size);

	@Query("""
			select count(p) from Project p join p.memberships m
			where m.id.personId.value = :person and p.deletedAt is null
				and lower(p.name.value) like lower(:pattern) escape '!'
			""")
	long countListings(@Param("person") UUID person, @Param("pattern") String pattern);

}
