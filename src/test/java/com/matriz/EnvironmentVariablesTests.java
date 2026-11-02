package com.matriz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
 * variables, never their values (ADR-007 decisions 6 and 10; ADR-005).
 */
@ExtendWith(OutputCaptureExtension.class)
class EnvironmentVariablesTests {

	private static final String KEY_RULE = "These environment variables must be a Base64-encoded key of at least 32 "
			+ "bytes, such as the output of openssl rand -base64 32: MATRIZ_JWT_SIGNING_KEY.";

	@Test
	void startUpStopsWhenTheRequiredVariablesAreMissing(CapturedOutput output) {
		// Blank command-line arguments override any .env file and real environment variable.
		assertThatThrownBy(() -> start("--MATRIZ_DB_URL=", "--MATRIZ_DB_USER=", "--MATRIZ_DB_PASSWORD=",
				"--MATRIZ_JWT_SIGNING_KEY=", "--MATRIZ_DEMO_DATA="))
			.isInstanceOf(EnvironmentVariablesException.class)
			.hasMessage("These environment variables are missing or blank: "
					+ "MATRIZ_DB_URL, MATRIZ_DB_USER, MATRIZ_DB_PASSWORD, MATRIZ_JWT_SIGNING_KEY.");
		assertThat(output).contains("APPLICATION FAILED TO START")
			.contains("Matriz cannot start. These environment variables are missing or blank: "
					+ "MATRIZ_DB_URL, MATRIZ_DB_USER, MATRIZ_DB_PASSWORD, MATRIZ_JWT_SIGNING_KEY.")
			.contains(EnvironmentVariablesFailureAnalyzer.ACTION);
	}

	@Test
	void startUpStopsWhenTheDemonstrationDataSwitchIsNotTrueOrFalse(CapturedOutput output) {
		assertThatThrownBy(() -> start("--MATRIZ_DB_URL=jdbc:postgresql://localhost:1/unused",
				"--MATRIZ_DB_USER=fictitious-user", "--MATRIZ_DB_PASSWORD=fictitious-secret-value",
				"--MATRIZ_JWT_SIGNING_KEY=" + DatabaseTestSupport.SIGNING_KEY, "--MATRIZ_DEMO_DATA=yes"))
			.isInstanceOf(EnvironmentVariablesException.class)
			.hasMessage("These environment variables must be true or false: MATRIZ_DEMO_DATA.");
		assertThat(output).contains("Matriz cannot start.")
			.doesNotContain("fictitious-secret-value")
			.doesNotContain("fictitious-user")
			.doesNotContain(DatabaseTestSupport.SIGNING_KEY);
	}

	@Test
	void startUpStopsWhenTheSigningKeyIsThePlaceholderOfTheExampleFile(CapturedOutput output) throws IOException {
		String placeholder = exampleValue("MATRIZ_JWT_SIGNING_KEY");
		assertThatThrownBy(() -> start("--MATRIZ_DB_URL=jdbc:postgresql://localhost:1/unused",
				"--MATRIZ_DB_USER=fictitious-user", "--MATRIZ_DB_PASSWORD=fictitious-secret-value",
				"--MATRIZ_JWT_SIGNING_KEY=" + placeholder, "--MATRIZ_DEMO_DATA=false"))
			.isInstanceOf(EnvironmentVariablesException.class)
			.hasMessage(KEY_RULE);
		assertThat(output).contains("Matriz cannot start. " + KEY_RULE)
			.contains(EnvironmentVariablesFailureAnalyzer.ACTION)
			.doesNotContain(placeholder)
			.doesNotContain("fictitious-secret-value");
	}

	@Test
	void eachRequiredVariableIsCheckedAndOnlyItsNameIsReported() {
		MockEnvironment environment = new MockEnvironment().withProperty("MATRIZ_DB_URL", "jdbc:postgresql://db/matriz")
			.withProperty("MATRIZ_DB_USER", " ")
			.withProperty("MATRIZ_DEMO_DATA", "maybe");
		assertThatThrownBy(() -> EnvironmentCheck.check(environment)).isInstanceOfSatisfying(
				EnvironmentVariablesException.class, (ex) -> {
					assertThat(ex.missing()).containsExactly("MATRIZ_DB_USER", "MATRIZ_DB_PASSWORD",
							"MATRIZ_JWT_SIGNING_KEY");
					assertThat(ex.invalid()).containsExactly("MATRIZ_DEMO_DATA");
					assertThat(ex.getMessage()).doesNotContain("jdbc:postgresql").doesNotContain("maybe");
				});
	}

	@Test
	void theSigningKeyMustBeABase64KeyOfAtLeast256Bits() throws IOException {
		String[] rejected = { exampleValue("MATRIZ_JWT_SIGNING_KEY"), "not a Base64 key",
				"-" + DatabaseTestSupport.randomKey(32).substring(1), DatabaseTestSupport.randomKey(31) };
		for (String key : rejected) {
			MockEnvironment environment = requiredVariables().withProperty("MATRIZ_JWT_SIGNING_KEY", key);
			assertThatThrownBy(() -> EnvironmentCheck.check(environment)).as("key %s", key)
				.isInstanceOfSatisfying(EnvironmentVariablesException.class, (ex) -> {
					assertThat(ex.invalidKeys()).containsExactly("MATRIZ_JWT_SIGNING_KEY");
					assertThat(ex.missing()).isEmpty();
					assertThat(ex.getMessage()).isEqualTo(KEY_RULE).doesNotContain(key);
				});
		}
		for (int bytes : new int[] { 32, 64 }) {
			MockEnvironment environment = requiredVariables().withProperty("MATRIZ_JWT_SIGNING_KEY",
					DatabaseTestSupport.randomKey(bytes));
			assertThatNoException().as("%d bytes", bytes).isThrownBy(() -> EnvironmentCheck.check(environment));
		}
	}

	@Test
	void theDemonstrationDataSwitchMayBeAbsentTrueOrFalseInAnyCase() {
		for (String value : new String[] { null, "", "true", "false", "TRUE", "False" }) {
			MockEnvironment environment = requiredVariables();
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
			MockEnvironment environment = requiredVariables();
			if (value != null) {
				environment.setProperty("MATRIZ_DEMO_DATA", value);
			}
			assertThatNoException().as("MATRIZ_DEMO_DATA=%s", value).isThrownBy(() -> EnvironmentCheck.check(environment));
		}
		for (String value : new String[] { "true", "True" }) {
			MockEnvironment environment = requiredVariables().withProperty("MATRIZ_DEMO_DATA", value)
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

	@Test
	void theDemonstrationPasswordsMustFollowThePasswordRulesOnlyWhenTheDemonstrationDataIsOn() {
		String fourteenCharacters = "fictitious-14c";
		String fifteenCharacters = "fictitious-15ch";
		String seventyThreeBytes = "f".repeat(73);
		// 14 characters but 28 UTF-16 code units: characters are counted as code points.
		String fourteenEmoji = Character.toString(0x1F600).repeat(14);
		// 36 characters and 72 bytes in UTF-8, the limit.
		String seventyTwoBytes = "\u00e9".repeat(36);
		MockEnvironment environment = requiredVariables().withProperty("MATRIZ_DEMO_DATA", "true")
			.withProperty("MATRIZ_DEMO_PASSWORD_ANA", fourteenCharacters)
			.withProperty("MATRIZ_DEMO_PASSWORD_RUI", fifteenCharacters)
			.withProperty("MATRIZ_DEMO_PASSWORD_EVA", seventyThreeBytes)
			.withProperty("MATRIZ_DEMO_PASSWORD_DANIEL", fourteenEmoji);
		assertThatThrownBy(() -> EnvironmentCheck.check(environment))
			.isInstanceOfSatisfying(EnvironmentVariablesException.class, (ex) -> {
				assertThat(ex.invalidPasswords()).containsExactly("MATRIZ_DEMO_PASSWORD_ANA", "MATRIZ_DEMO_PASSWORD_EVA",
						"MATRIZ_DEMO_PASSWORD_DANIEL");
				assertThat(ex.missing()).isEmpty();
				assertThat(ex.getMessage()).isEqualTo("These environment variables must have at least 15 characters "
						+ "and at most 72 bytes in UTF-8: MATRIZ_DEMO_PASSWORD_ANA, MATRIZ_DEMO_PASSWORD_EVA, "
						+ "MATRIZ_DEMO_PASSWORD_DANIEL.")
					.doesNotContain(fourteenCharacters)
					.doesNotContain(seventyThreeBytes)
					.doesNotContain(fourteenEmoji);
			});

		environment.setProperty("MATRIZ_DEMO_PASSWORD_ANA", seventyTwoBytes);
		environment.setProperty("MATRIZ_DEMO_PASSWORD_EVA", fifteenCharacters);
		environment.setProperty("MATRIZ_DEMO_PASSWORD_DANIEL", Character.toString(0x1F600).repeat(15));
		assertThatNoException().isThrownBy(() -> EnvironmentCheck.check(environment));

		environment.setProperty("MATRIZ_DEMO_DATA", "false");
		environment.setProperty("MATRIZ_DEMO_PASSWORD_ANA", fourteenCharacters);
		assertThatNoException().isThrownBy(() -> EnvironmentCheck.check(environment));
	}

	private static MockEnvironment requiredVariables() {
		return new MockEnvironment().withProperty("MATRIZ_DB_URL", "jdbc:postgresql://db/matriz")
			.withProperty("MATRIZ_DB_USER", "fictitious-user")
			.withProperty("MATRIZ_DB_PASSWORD", "fictitious-secret-value")
			.withProperty("MATRIZ_JWT_SIGNING_KEY", DatabaseTestSupport.SIGNING_KEY);
	}

	/** The value of a variable in .env.example, which start-up must reject where it is a placeholder. */
	private static String exampleValue(String name) throws IOException {
		return Files.readAllLines(Path.of(".env.example"))
			.stream()
			.filter((line) -> line.startsWith(name + "="))
			.map((line) -> line.substring(name.length() + 1))
			.findFirst()
			.orElseThrow(() -> new AssertionError(name + " is not in .env.example"));
	}

	private static void start(String... args) {
		new SpringApplicationBuilder(MatrizApplication.class).web(WebApplicationType.NONE).run(args).close();
	}

}
