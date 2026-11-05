package com.matriz.identity.application;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.matriz.identity.PersonDirectory;
import com.matriz.identity.PersonId;
import com.matriz.identity.domain.Person;
import com.matriz.identity.domain.PersonRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tells other modules the display names of people, and nothing else about them (Assumption A-017). */
@Service
class LookUpDisplayNames implements PersonDirectory {

	private final PersonRepository people;

	LookUpDisplayNames(PersonRepository people) {
		this.people = people;
	}

	@Override
	@Transactional(readOnly = true)
	public Map<PersonId, String> displayNames(Collection<PersonId> ids) {
		Map<PersonId, String> names = new HashMap<>();
		if (ids.isEmpty()) {
			return names;
		}
		for (Person person : this.people.findAllByIdIn(ids)) {
			names.put(person.id(), person.displayName().value());
		}
		return names;
	}

}
