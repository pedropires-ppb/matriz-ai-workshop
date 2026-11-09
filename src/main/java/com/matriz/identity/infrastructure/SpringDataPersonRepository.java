package com.matriz.identity.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.matriz.identity.PersonId;
import com.matriz.identity.domain.Person;
import com.matriz.identity.domain.PersonRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** {@link PersonRepository} with Spring Data JPA (ADR-008 decision 4). */
interface SpringDataPersonRepository extends PersonRepository, Repository<Person, PersonId> {

	@Override
	default List<Person> findOthers(Collection<PersonId> excluded, String nameContains) {
		return selectOthers(excluded.stream().map(PersonId::value).toList(), nameContains);
	}

	/**
	 * {@code locate} finds the text as it is, so no character of it is a wildcard; both sides are
	 * lower-cased by the database, and the empty text is found in every name.
	 */
	@Query("""
			select p from Person p
			where p.id.value not in :excluded
				and locate(lower(:text), lower(p.displayName.value)) > 0
			order by lower(p.displayName.value), p.id.value
			""")
	List<Person> selectOthers(@Param("excluded") List<UUID> excluded, @Param("text") String nameContains);

}
