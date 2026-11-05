package com.matriz.identity;

import java.util.Collection;
import java.util.Map;

/**
 * What other modules may learn about people: the display names, to show them (openapi.yaml,
 * PersonSummary). Never the login name or the password hash (Assumption A-017).
 */
public interface PersonDirectory {

	/** The display name of each of these people who is known; an unknown person is left out. */
	Map<PersonId, String> displayNames(Collection<PersonId> people);

}
