package com.matriz;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
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
 * connection, is created. The signing key must be a Base64-encoded key of at least 256 bits
 * (ADR-005). The demonstration passwords are required, and must follow the password rules of
 * ADR-005, only when {@code MATRIZ_DEMO_DATA=true}. Runs after test-supplied properties are
 * registered, which an EnvironmentPostProcessor would not see.
 */
@Component
class EnvironmentCheck implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

	static final String SIGNING_KEY = "MATRIZ_JWT_SIGNING_KEY";

	static final List<String> REQUIRED = List.of("MATRIZ_DB_URL", "MATRIZ_DB_USER", "MATRIZ_DB_PASSWORD",
			SIGNING_KEY);

	static final String DEMO_DATA = "MATRIZ_DEMO_DATA";

	/** 256 bits, the minimum key size for HS256 (RFC 7518, section 3.2). */
	static final int SIGNING_KEY_MINIMUM_BYTES = 32;

	static final int PASSWORD_MINIMUM_CHARACTERS = 15;

	/** BCrypt's limit. */
	static final int PASSWORD_MAXIMUM_BYTES = 72;

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
		List<String> invalidKeys = new ArrayList<>();
		String signingKey = environment.getProperty(SIGNING_KEY);
		if (StringUtils.hasText(signingKey) && !isSigningKey(signingKey)) {
			invalidKeys.add(SIGNING_KEY);
		}
		List<String> invalidPasswords = new ArrayList<>();
		if (DemoDataMigrations.enabled(environment)) {
			for (String name : DemoDataMigrations.PASSWORD_VARIABLES) {
				String password = environment.getProperty(name);
				if (!StringUtils.hasText(password)) {
					missing.add(name);
				}
				else if (!followsThePasswordRules(password)) {
					invalidPasswords.add(name);
				}
			}
		}
		if (!missing.isEmpty() || !invalid.isEmpty() || !invalidKeys.isEmpty() || !invalidPasswords.isEmpty()) {
			throw new EnvironmentVariablesException(missing, invalid, invalidKeys, invalidPasswords);
		}
	}

	static boolean isSigningKey(String value) {
		try {
			return Base64.getDecoder().decode(value).length >= SIGNING_KEY_MINIMUM_BYTES;
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

	/** Characters are counted as Unicode code points. */
	static boolean followsThePasswordRules(String password) {
		return password.codePointCount(0, password.length()) >= PASSWORD_MINIMUM_CHARACTERS
				&& password.getBytes(StandardCharsets.UTF_8).length <= PASSWORD_MAXIMUM_BYTES;
	}

}
