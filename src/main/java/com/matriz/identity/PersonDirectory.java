package com.matriz.identity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * What other modules may learn about people: the display names, to show them, and the people who
 * are not in a set, to choose whom to add to a project (openapi.yaml, PersonSummary). Never the login name or the password hash (Assumption A-017).
 */
public interface PersonDirectory {

	/** The display name of each of these people who is known; an unknown person is left out. */
	Map<PersonId, String> displayNames(Collection<PersonId> people);

	/**
	 * The people known to Matriz, except these, whose display name contains the text ignoring letter
	 * case, or all of them when the text is {@code null}; ordered by display name ignoring letter case,
	 * then by identifier (Assumption A-030).
	 */
	List<NamedPerson> findOthers(Collection<PersonId> excluded, String nameContains);

}
