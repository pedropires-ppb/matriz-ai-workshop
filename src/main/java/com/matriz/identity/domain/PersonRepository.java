package com.matriz.identity.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.matriz.identity.PersonId;

/** The people known to Matriz (ADR-008 decision 4). */
public interface PersonRepository {

	Optional<Person> findById(PersonId id);

	Optional<Person> findByLoginName(LoginName loginName);

	/** The people with these identifiers who are known; an unknown identifier is left out. */
	List<Person> findAllByIdIn(Collection<PersonId> ids);

	/**
	 * The people not in {@code excluded} whose display name contains the text, ignoring letter case;
	 * the empty text selects everyone. Ordered by display name ignoring letter case, then by identifier.
	 */
	List<Person> findOthers(Collection<PersonId> excluded, String nameContains);

}
