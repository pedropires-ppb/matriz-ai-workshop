package com.matriz;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Starts the application against its own empty PostgreSQL database, with the demonstration data
 * off: tests create the data they need (ADR-007 decision 10). The access tokens are signed with a
 * key generated at each test run, so no key is kept in the repository (ADR-005).
 */
@SpringBootTest
abstract class DatabaseTestSupport {

	static final String SIGNING_KEY = randomSigningKey();

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		String url = SharedPostgres.newDatabase();
		registry.add("MATRIZ_DB_URL", () -> url);
		registry.add("MATRIZ_DB_USER", SharedPostgres::username);
		registry.add("MATRIZ_DB_PASSWORD", SharedPostgres::password);
		registry.add("MATRIZ_JWT_SIGNING_KEY", () -> SIGNING_KEY);
		registry.add("MATRIZ_DEMO_DATA", () -> "false");
		registry.add("spring.datasource.hikari.maximum-pool-size", () -> "2");
	}

	/** A Base64-encoded random key of the given number of bytes. */
	static String randomKey(int bytes) {
		byte[] key = new byte[bytes];
		new SecureRandom().nextBytes(key);
		return Base64.getEncoder().encodeToString(key);
	}

	private static String randomSigningKey() {
		return randomKey(32);
	}

}
