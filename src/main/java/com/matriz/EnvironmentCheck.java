package com.matriz;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Checks the environment variables of ADR-007 decision 6 before any bean, including the database
 * connection, is created. The demonstration passwords are required only when
 * {@code MATRIZ_DEMO_DATA=true}. Runs after test-supplied properties are registered, which an
 * EnvironmentPostProcessor would not see.
 */
@Component
class EnvironmentCheck implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

	static final List<String> REQUIRED = List.of("MATRIZ_DB_URL", "MATRIZ_DB_USER", "MATRIZ_DB_PASSWORD");

	static final String DEMO_DATA = "MATRIZ_DEMO_DATA";

	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		check(this.environment);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	static void check(Environment environment) {
		List<String> missing = new ArrayList<>();
		for (String name : REQUIRED) {
			if (!StringUtils.hasText(environment.getProperty(name))) {
				missing.add(name);
			}
		}
		List<String> invalid = new ArrayList<>();
		String demoData = environment.getProperty(DEMO_DATA);
		if (StringUtils.hasText(demoData) && !"true".equalsIgnoreCase(demoData) && !"false".equalsIgnoreCase(demoData)) {
			invalid.add(DEMO_DATA);
		}
		if (DemoDataMigrations.enabled(environment)) {
			for (String name : DemoDataMigrations.PASSWORD_VARIABLES) {
				if (!StringUtils.hasText(environment.getProperty(name))) {
					missing.add(name);
				}
			}
		}
		if (!missing.isEmpty() || !invalid.isEmpty()) {
			throw new EnvironmentVariablesException(missing, invalid);
		}
	}

}
