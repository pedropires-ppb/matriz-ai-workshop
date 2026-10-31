package com.matriz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A missing, blank, or invalid environment variable stops start-up with a message that names the
 * variables, never their values (ADR-007 decisions 6 and 10).
 */
@ExtendWith(OutputCaptureExtension.class)
class EnvironmentVariablesTests {

	@Test
	void startUpStopsWhenTheDatabaseVariablesAreMissing(CapturedOutput output) {
		// Blank command-line arguments override any .env file and real environment variable.
		assertThatThrownBy(() -> start("--MATRIZ_DB_URL=", "--MATRIZ_DB_USER=", "--MATRIZ_DB_PASSWORD=",
				"--MATRIZ_DEMO_DATA="))
			.isInstanceOf(EnvironmentVariablesException.class)
			.hasMessage("These environment variables are missing or blank: "
					+ "MATRIZ_DB_URL, MATRIZ_DB_USER, MATRIZ_DB_PASSWORD.");
		assertThat(output).contains("APPLICATION FAILED TO START")
			.contains("Matriz cannot start. These environment variables are missing or blank: "
					+ "MATRIZ_DB_URL, MATRIZ_DB_USER, MATRIZ_DB_PASSWORD.")
			.contains(EnvironmentVariablesFailureAnalyzer.ACTION);
	}

	@Test
	void startUpStopsWhenTheDemonstrationDataSwitchIsNotTrueOrFalse(CapturedOutput output) {
		assertThatThrownBy(() -> start("--MATRIZ_DB_URL=jdbc:postgresql://localhost:1/unused",
				"--MATRIZ_DB_USER=fictitious-user", "--MATRIZ_DB_PASSWORD=fictitious-secret-value",
				"--MATRIZ_DEMO_DATA=yes"))
			.isInstanceOf(EnvironmentVariablesException.class)
			.hasMessage("These environment variables must be true or false: MATRIZ_DEMO_DATA.");
		assertThat(output).contains("Matriz cannot start.")
			.doesNotContain("fictitious-secret-value")
			.doesNotContain("fictitious-user");
	}

	@Test
	void eachRequiredVariableIsCheckedAndOnlyItsNameIsReported() {
		MockEnvironment environment = new MockEnvironment().withProperty("MATRIZ_DB_URL", "jdbc:postgresql://db/matriz")
			.withProperty("MATRIZ_DB_USER", " ")
			.withProperty("MATRIZ_DEMO_DATA", "maybe");
		assertThatThrownBy(() -> EnvironmentCheck.check(environment)).isInstanceOfSatisfying(
				EnvironmentVariablesException.class, (ex) -> {
					assertThat(ex.missing()).containsExactly("MATRIZ_DB_USER", "MATRIZ_DB_PASSWORD");
					assertThat(ex.invalid()).containsExactly("MATRIZ_DEMO_DATA");
					assertThat(ex.getMessage()).doesNotContain("jdbc:postgresql").doesNotContain("maybe");
				});
	}

	@Test
	void theDemonstrationDataSwitchMayBeAbsentTrueOrFalseInAnyCase() {
		for (String value : new String[] { null, "", "true", "false", "TRUE", "False" }) {
			MockEnvironment environment = databaseVariables();
			DemoDataMigrations.PASSWORD_VARIABLES.forEach((name) -> environment.setProperty(name, "fictitious-password"));
			if (value != null) {
				environment.setProperty("MATRIZ_DEMO_DATA", value);
			}
			assertThatNoException().as("MATRIZ_DEMO_DATA=%s", value).isThrownBy(() -> EnvironmentCheck.check(environment));
		}
	}

	@Test
	void theDemonstrationPasswordsAreRequiredOnlyWhenTheDemonstrationDataIsOn() {
		for (String value : new String[] { null, "", "false", "FALSE" }) {
			MockEnvironment environment = databaseVariables();
			if (value != null) {
				environment.setProperty("MATRIZ_DEMO_DATA", value);
			}
			assertThatNoException().as("MATRIZ_DEMO_DATA=%s", value).isThrownBy(() -> EnvironmentCheck.check(environment));
		}
		for (String value : new String[] { "true", "True" }) {
			MockEnvironment environment = databaseVariables().withProperty("MATRIZ_DEMO_DATA", value)
				.withProperty("MATRIZ_DEMO_PASSWORD_ANA", "fictitious-password")
				.withProperty("MATRIZ_DEMO_PASSWORD_RUI", " ");
			assertThatThrownBy(() -> EnvironmentCheck.check(environment)).as("MATRIZ_DEMO_DATA=%s", value)
				.isInstanceOfSatisfying(EnvironmentVariablesException.class, (ex) -> {
					assertThat(ex.missing()).containsExactly("MATRIZ_DEMO_PASSWORD_RUI", "MATRIZ_DEMO_PASSWORD_EVA",
							"MATRIZ_DEMO_PASSWORD_DANIEL");
					assertThat(ex.invalid()).isEmpty();
					assertThat(ex.getMessage()).doesNotContain("fictitious-password");
				});
		}
	}

	private static MockEnvironment databaseVariables() {
		return new MockEnvironment().withProperty("MATRIZ_DB_URL", "jdbc:postgresql://db/matriz")
			.withProperty("MATRIZ_DB_USER", "fictitious-user")
			.withProperty("MATRIZ_DB_PASSWORD", "fictitious-secret-value");
	}

	private static void start(String... args) {
		new SpringApplicationBuilder(MatrizApplication.class).web(WebApplicationType.NONE).run(args).close();
	}

}
