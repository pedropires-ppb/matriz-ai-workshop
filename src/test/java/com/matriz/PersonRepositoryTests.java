package com.matriz;

import java.util.List;
import java.util.UUID;

import com.matriz.identity.PersonId;
import com.matriz.identity.domain.DisplayName;
import com.matriz.identity.domain.LoginName;
import com.matriz.identity.domain.PasswordHash;
import com.matriz.identity.domain.Person;
import com.matriz.identity.domain.PersonRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How a person is stored (ADR-008, open point "Mapping conventions"): a person stored and then
 * read in a new persistence context keeps every field, each in its column of the schema
 * (docs/database.md).
 */
class PersonRepositoryTests extends DatabaseTestSupport {

	@Autowired
	private PersonRepository people;

	@Autowired
	private TransactionTemplate transactions;

	@Autowired
	private JdbcClient jdbc;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	void aStoredPersonIsReadInANewPersistenceContextWithEveryField() {
		Person stored = store(newPerson());

		Person byId = this.transactions.execute((transaction) -> this.people.findById(stored.id()).orElseThrow());
		Person byLoginName = this.transactions
			.execute((transaction) -> this.people.findByLoginName(stored.loginName()).orElseThrow());

		for (Person read : List.of(byId, byLoginName)) {
			assertThat(read).isNotSameAs(stored);
			assertThat(read.id()).isEqualTo(stored.id());
			assertThat(read.loginName()).isEqualTo(stored.loginName());
			assertThat(read.displayName()).isEqualTo(stored.displayName());
			assertThat(read.passwordHash()).isEqualTo(stored.passwordHash());
		}
	}

	@Test
	void eachFieldIsStoredInItsColumn() {
		Person stored = store(newPerson());

		assertThat(this.jdbc.sql("SELECT login_name, display_name, password_hash FROM person WHERE person_id = ?")
			.param(stored.id().value())
			.query((row, number) -> List.of(row.getString(1), row.getString(2), row.getString(3)))
			.single()).containsExactly(stored.loginName().value(), stored.displayName().value(),
					stored.passwordHash().value());
	}

	@Test
	void unknownValuesFindNobody() {
		assertThat(this.people.findById(new PersonId(UUID.randomUUID()))).isEmpty();
		assertThat(this.people.findByLoginName(new LoginName("test-nobody-" + UUID.randomUUID()))).isEmpty();
	}

	private Person store(Person person) {
		this.transactions.executeWithoutResult((transaction) -> this.entityManager.persist(person));
		return person;
	}

	private static Person newPerson() {
		UUID id = UUID.randomUUID();
		return new Person(new PersonId(id), new LoginName("test-mapping-" + id), new DisplayName("Rui Martins"),
				new PasswordHash("{bcrypt}fictitious-hash-" + id));
	}

}
