package com.matriz;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.ANNOTATIONS;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.ENUMS;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The layers inside each module (ADR-008). The module boundaries themselves are checked by
 * {@link ModuleBoundaryTests}. In the package patterns, {@code *} is one module and {@code ..} any
 * sub-package. The layers are empty in I1, so the rules allow it.
 */
class LayeringTests {

	private static final String MODULE_API = "com.matriz.*";

	private static final String DOMAIN = "com.matriz.*.domain..";

	private static final String APPLICATION = "com.matriz.*.application..";

	private static final String INFRASTRUCTURE = "com.matriz.*.infrastructure..";

	private static final String WEB = "com.matriz.*.web..";

	/**
	 * Domain classes carry Jakarta Persistence's mapping annotations (ADR-008, decision 4). An
	 * annotation's attributes may name an enumeration, such as {@code EnumType}; anything else from
	 * Jakarta Persistence, such as {@code EntityManager}, stays outside the domain.
	 */
	private static final DescribedPredicate<JavaClass> JAKARTA_PERSISTENCE_BEYOND_MAPPING_ANNOTATIONS = resideInAPackage(
			"jakarta.persistence..")
		.and(not(ANNOTATIONS))
		.and(not(ENUMS))
		.as("are Jakarta Persistence classes other than annotations and enumerations");

	private static final JavaClasses CLASSES = new ClassFileImporter()
		.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
		.importPackages("com.matriz");

	@Test
	void theDomainUsesNoOtherLayerAndOnlyTheMappingAnnotationsOfJakartaPersistence() {
		check(noClasses().that()
			.resideInAPackage(DOMAIN)
			.should()
			.dependOnClassesThat(resideInAnyPackage(APPLICATION, INFRASTRUCTURE, WEB, "org.springframework..",
					"org.hibernate..")
				.or(JAKARTA_PERSISTENCE_BEYOND_MAPPING_ANNOTATIONS))
			.because("the domain uses no other layer, nor Spring or Hibernate, and of Jakarta Persistence only "
					+ "its mapping annotations (ADR-008)"));
	}

	@Test
	void theApplicationLayerUsesNeitherInfrastructureNorWeb() {
		check(noClasses().that()
			.resideInAPackage(APPLICATION)
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage(INFRASTRUCTURE, WEB)
			.because("use cases reach persistence only through the domain's repository interfaces (ADR-008)"));
	}

	@Test
	void nothingOutsideTheWebLayerUsesIt() {
		check(noClasses().that()
			.resideOutsideOfPackage(WEB)
			.should()
			.dependOnClassesThat()
			.resideInAPackage(WEB)
			.because("no layer depends on the web layer (ADR-008)"));
	}

	@Test
	void theWebLayerDoesNotUseInfrastructure() {
		check(noClasses().that()
			.resideInAPackage(WEB)
			.should()
			.dependOnClassesThat()
			.resideInAPackage(INFRASTRUCTURE)
			.because("the web layer calls the application layer (ADR-008)"));
	}

	@Test
	void aModuleApiUsesNoLayer() {
		check(noClasses().that()
			.resideInAPackage(MODULE_API)
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage(DOMAIN, APPLICATION, INFRASTRUCTURE, WEB)
			.because("a module's top-level package is its API, and its layers are internal (ADR-008)"));
	}

	private static void check(ArchRule rule) {
		rule.allowEmptyShould(true).check(CLASSES);
	}

}
