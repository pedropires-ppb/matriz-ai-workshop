package com.matriz;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module boundaries (ADR-001; architecture definition, module rules; ADR-007 decision 11). The
 * build fails if a module uses another module's internals or a dependency it is not allowed. The
 * layers inside each module are checked by {@link LayeringTests} (ADR-008).
 */
class ModuleBoundaryTests {

	/**
	 * Only Identity learns who is acting: other modules take a {@code SignedInPerson} parameter
	 * (ADR-009 decision 3). Password hashing is allowed everywhere, because it cannot learn or set
	 * who is acting (clarified on 2026-11-02, Lab Worksheet 03, question 31).
	 */
	static final ArchRule ONLY_IDENTITY_USES_SPRING_SECURITY = noClasses().that()
		.resideOutsideOfPackage("com.matriz.identity..")
		.should()
		.dependOnClassesThat(resideInAPackage("org.springframework.security..")
			.and(not(resideInAPackage("org.springframework.security.crypto.."))))
		.because("only Identity learns who is acting; other modules take a SignedInPerson parameter "
				+ "(ADR-009 decision 3)");

	private final ApplicationModules modules = ApplicationModules.of(MatrizApplication.class);

	@Test
	void everyModuleKeepsToItsAllowedDependencies() {
		this.modules.verify();
	}

	@Test
	void theFiveModulesAreDetected() {
		assertThat(this.modules.stream().map(ApplicationModule::getIdentifier).map(Object::toString))
			.containsExactlyInAnyOrder("identity", "projects", "tasks", "progress", "accesscontrol");
	}

	@Test
	void onlyIdentityUsesSpringSecurity() {
		ONLY_IDENTITY_USES_SPRING_SECURITY.check(new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("com.matriz"));
	}

	@Test
	void theSpringSecurityRuleRefusesAnyUseButPasswordHashing() {
		JavaClasses classes = new ClassFileImporter().importClasses(ReadsTheSecurityContext.class,
				HashesAPassword.class);

		assertThat(ONLY_IDENTITY_USES_SPRING_SECURITY.evaluate(classes).getFailureReport().getDetails())
			.isNotEmpty()
			.allMatch((violation) -> violation.contains(ReadsTheSecurityContext.class.getName()));
	}

	/** Outside Identity, and learns who is acting from Spring Security: the rule refuses it. */
	static class ReadsTheSecurityContext {

		Authentication actingPerson() {
			return SecurityContextHolder.getContext().getAuthentication();
		}

	}

	/** Outside Identity, and only hashes a password: the rule allows it. */
	static class HashesAPassword {

		String hash(String password) {
			return PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(password);
		}

	}

}
