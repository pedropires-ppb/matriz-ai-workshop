package com.matriz;

import org.junit.jupiter.api.Test;

import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module boundaries (ADR-001; architecture definition, module rules; ADR-007 decision 11). The
 * build fails if a module uses another module's internals or a dependency it is not allowed. The
 * layers inside each module are checked by {@link LayeringTests} (ADR-008).
 */
class ModuleBoundaryTests {

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

}
