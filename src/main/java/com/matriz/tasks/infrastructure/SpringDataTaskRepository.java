package com.matriz.tasks.infrastructure;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.matriz.tasks.TaskId;
import com.matriz.tasks.TaskPriority;
import com.matriz.tasks.TaskState;
import com.matriz.tasks.domain.Task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA's side of {@link JpaTaskRepository} (ADR-008 decision 4). Every query leaves out
 * deleted tasks (Assumption A-022). The list is ordered by the technical creation order, which is
 * never exposed (ADR-008, mapping of {@code creation_order}).
 */
interface SpringDataTaskRepository extends Repository<Task, TaskId> {

	Task save(Task task);

	@Query("select t from Task t where t.id.value = :id and t.projectId.value = :project and t.deletedAt is null")
	Optional<Task> findVisible(@Param("project") UUID project, @Param("id") UUID id);

	@Query(value = """
			select t from Task t
			where t.projectId.value = :project and t.deletedAt is null
				and t.state in :states and t.priority in :priorities
			order by t.creationOrder
			""", countQuery = """
			select count(t) from Task t
			where t.projectId.value = :project and t.deletedAt is null
				and t.state in :states and t.priority in :priorities
			""")
	Page<Task> findPage(@Param("project") UUID project, @Param("states") Collection<TaskState> states,
			@Param("priorities") Collection<TaskPriority> priorities, Pageable pageable);

}
