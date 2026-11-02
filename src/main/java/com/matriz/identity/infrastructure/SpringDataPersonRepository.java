package com.matriz.identity.infrastructure;

import com.matriz.identity.PersonId;
import com.matriz.identity.domain.Person;
import com.matriz.identity.domain.PersonRepository;

import org.springframework.data.repository.Repository;

/** {@link PersonRepository} with Spring Data JPA (ADR-008 decision 4). */
interface SpringDataPersonRepository extends PersonRepository, Repository<Person, PersonId> {

}
