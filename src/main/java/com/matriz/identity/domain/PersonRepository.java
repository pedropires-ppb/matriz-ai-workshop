package com.matriz.identity.domain;

import java.util.Optional;

import com.matriz.identity.PersonId;

/** The people known to Matriz (ADR-008 decision 4). */
public interface PersonRepository {

	Optional<Person> findById(PersonId id);

	Optional<Person> findByLoginName(LoginName loginName);

}
