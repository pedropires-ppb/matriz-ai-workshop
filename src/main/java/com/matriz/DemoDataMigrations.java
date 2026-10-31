package com.matriz;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Applies the demonstration data as Flyway migrations (ADR-007 decision 10, amended on 2026-10-30).
 * <ul>
 * <li>The migrations in {@code db/demo} are added to Flyway's locations only when
 * {@code MATRIZ_DEMO_DATA=true}. They are numbered as sub-versions of the schema migration they
 * were written for, for example {@code V1_1} after {@code V1}, and share Flyway's history table, so
 * each runs once per database.</li>
 * <li>The demonstration passwords are hashed at each start with demonstration data, and only the
 * hashes reach the migrations, as Flyway placeholders (ADR-005 decision 6).</li>
 * <li>The setting is fixed for each database. Before migrating, start-up stops if the setting does
 * not match the database.</li>
 * </ul>
 * The beans are always registered, because the conditions are evaluated before
 * {@link EnvironmentCheck} has checked the setting.
 */
@Configuration(proxyBeanMethods = false)
class DemoDataMigrations {

	static final String LOCATION = "classpath:db/demo";

	static final List<String> PASSWORD_VARIABLES = List.of("MATRIZ_DEMO_PASSWORD_ANA", "MATRIZ_DEMO_PASSWORD_RUI",
			"MATRIZ_DEMO_PASSWORD_EVA", "MATRIZ_DEMO_PASSWORD_DANIEL");

	private static final String PASSWORD_VARIABLE_PREFIX = "MATRIZ_DEMO_PASSWORD_";

	private static final String RESET = "reset the local database as described in README.md (this deletes all its data)";

	private static final Logger log = LoggerFactory.getLogger(DemoDataMigrations.class);

	@Bean
	FlywayConfigurationCustomizer demoDataLocation(Environment environment) {
		return (configuration) -> {
			if (!enabled(environment)) {
				return;
			}
			List<Location> locations = new ArrayList<>(Arrays.asList(configuration.getLocations()));
			locations.add(new Location(LOCATION));
			Map<String, String> placeholders = new HashMap<>(configuration.getPlaceholders());
			PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
			for (String variable : PASSWORD_VARIABLES) {
				placeholders.put(placeholder(variable), passwordEncoder.encode(environment.getRequiredProperty(variable)));
			}
			configuration.locations(locations.toArray(Location[]::new)).placeholders(placeholders);
			log.info("Demonstration data is on: the migrations in {} are applied once to this database.", LOCATION);
		};
	}

	@Bean
	FlywayMigrationStrategy demoDataCheck(Environment environment) {
		return (flyway) -> migrate(flyway, enabled(environment));
	}

	static boolean enabled(Environment environment) {
		return "true".equalsIgnoreCase(environment.getProperty(EnvironmentCheck.DEMO_DATA));
	}

	/** The Flyway placeholder for the hash of a password variable, e.g. {@code demo_password_hash_ana}. */
	static String placeholder(String passwordVariable) {
		return "demo_password_hash_"
				+ passwordVariable.substring(PASSWORD_VARIABLE_PREFIX.length()).toLowerCase(Locale.ROOT);
	}

	/** Demonstration migrations have sub-versions such as 1.1; schema migrations have whole numbers. */
	static boolean isDemonstration(MigrationInfo migration) {
		return migration.getVersion() != null && migration.getVersion().getVersion().contains(".");
	}

	static void migrate(Flyway flyway, boolean demoData) {
		MigrationInfoService info = flyway.info();
		if (demoData) {
			checkDemonstrationDataCanBeAdded(info);
		}
		else {
			checkNoDemonstrationData(info);
		}
		List<MigrationInfo> pending = List.of(info.pending());
		try {
			flyway.migrate();
		}
		catch (FlywayException ex) {
			throw demonstrationFailure(flyway, pending, ex);
		}
	}

	private static void checkNoDemonstrationData(MigrationInfoService info) {
		for (MigrationInfo migration : info.applied()) {
			if (isDemonstration(migration)) {
				throw new DemoDataException("The demonstration data (migration " + migration.getVersion()
						+ ") was added to this database, but " + EnvironmentCheck.DEMO_DATA
						+ " is not true. The setting is fixed for each database.",
						"Set " + EnvironmentCheck.DEMO_DATA + "=true and the demonstration passwords ("
								+ String.join(", ", PASSWORD_VARIABLES) + "), or " + RESET + ".");
			}
		}
	}

	private static void checkDemonstrationDataCanBeAdded(MigrationInfoService info) {
		MigrationVersion latest = null;
		for (MigrationInfo migration : info.applied()) {
			if (migration.getVersion() != null && (latest == null || migration.getVersion().isNewerThan(latest))) {
				latest = migration.getVersion();
			}
		}
		for (MigrationInfo migration : info.all()) {
			if (isDemonstration(migration) && !migration.getState().isApplied() && latest != null
					&& latest.isNewerThan(migration.getVersion())) {
				throw new DemoDataException("The demonstration data (migration " + migration.getVersion()
						+ ") cannot be added to this database, because its schema is already at version " + latest
						+ ". The setting is fixed for each database.",
						"Set " + EnvironmentCheck.DEMO_DATA + "=false, or " + RESET + ".");
			}
		}
	}

	/**
	 * Flyway's message for a failed statement contains the statement, which for a demonstration
	 * migration contains the password hashes. Such a failure is therefore replaced by an exception
	 * that keeps only the database's error, without Flyway's exception as its cause.
	 */
	private static RuntimeException demonstrationFailure(Flyway flyway, List<MigrationInfo> pending,
			FlywayException ex) {
		SQLException sqlException = sqlException(ex);
		if (sqlException == null || pending.stream().noneMatch(DemoDataMigrations::isDemonstration)) {
			return ex;
		}
		MigrationInfo failed = null;
		Exception unknown = null;
		try {
			failed = firstNotApplied(flyway, pending);
		}
		catch (RuntimeException infoFailure) {
			unknown = infoFailure;
		}
		if (failed != null && !isDemonstration(failed)) {
			return ex;
		}
		String migration = (failed != null) ? "(migration " + failed.getVersion() + ") " : "";
		DemoDataException demo = new DemoDataException("The demonstration data " + migration
				+ "cannot be added to this database: " + firstLine(sqlException.getMessage()) + " (SQL state "
				+ sqlException.getSQLState() + "). None of it was added.",
				"Change or remove the rows that conflict with the demonstration data, set " + EnvironmentCheck.DEMO_DATA
						+ "=false, or " + RESET + ".");
		if (unknown != null) {
			demo.addSuppressed(unknown);
		}
		return demo;
	}

	private static MigrationInfo firstNotApplied(Flyway flyway, List<MigrationInfo> pending) {
		MigrationInfo[] applied = flyway.info().applied();
		for (MigrationInfo migration : pending) {
			boolean nowApplied = Arrays.stream(applied)
				.anyMatch((candidate) -> candidate.getVersion() != null
						&& candidate.getVersion().equals(migration.getVersion()));
			if (!nowApplied) {
				return migration;
			}
		}
		return null;
	}

	private static SQLException sqlException(Throwable ex) {
		for (Throwable candidate = ex; candidate != null; candidate = candidate.getCause()) {
			if (candidate instanceof SQLException sqlException) {
				return sqlException;
			}
		}
		return null;
	}

	private static String firstLine(String message) {
		if (message == null) {
			return "no message";
		}
		int end = message.indexOf('\n');
		return ((end >= 0) ? message.substring(0, end) : message).strip();
	}

}
