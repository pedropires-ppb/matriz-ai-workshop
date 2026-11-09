package com.matriz.identity;

import java.util.Objects;

/** A person as other modules may show them: the identifier and the display name only (Assumption A-017). */
public record NamedPerson(PersonId personId, String displayName) {

	public NamedPerson {
		Objects.requireNonNull(personId, "A person identifier is required.");
		Objects.requireNonNull(displayName, "A display name is required.");
	}

}
