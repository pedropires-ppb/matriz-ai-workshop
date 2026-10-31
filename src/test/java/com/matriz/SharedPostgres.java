package com.matriz;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One PostgreSQL container for the whole test run, with an empty database for each application
 * started by a test. Testcontainers removes the container when the tests end.
 */
final class SharedPostgres {

	static final String IMAGE = "postgres:18.6";

	private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer(IMAGE);

	private static final AtomicInteger DATABASES = new AtomicInteger();

	private SharedPostgres() {
	}

	/** Creates an empty database and returns its JDBC URL. */
	static String newDatabase() {
		PostgreSQLContainer container = container();
		String name = "matriz_test_" + DATABASES.incrementAndGet();
		try (Connection connection = DriverManager.getConnection(container.getJdbcUrl(), username(), password());
				Statement statement = connection.createStatement()) {
			statement.execute("CREATE DATABASE " + name);
		}
		catch (SQLException ex) {
			throw new IllegalStateException("Cannot create the test database " + name, ex);
		}
		return "jdbc:postgresql://" + container.getHost() + ":"
				+ container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) + "/" + name;
	}

	static String username() {
		return container().getUsername();
	}

	static String password() {
		return container().getPassword();
	}

	private static synchronized PostgreSQLContainer container() {
		if (!CONTAINER.isRunning()) {
			CONTAINER.start();
		}
		return CONTAINER;
	}

}
