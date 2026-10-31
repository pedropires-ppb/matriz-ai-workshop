package com.matriz;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Starts the application against its own empty PostgreSQL database, with the demonstration data
 * off: tests create the data they need (ADR-007 decision 10).
 */
@SpringBootTest
abstract class DatabaseTestSupport {

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		String url = SharedPostgres.newDatabase();
		registry.add("MATRIZ_DB_URL", () -> url);
		registry.add("MATRIZ_DB_USER", SharedPostgres::username);
		registry.add("MATRIZ_DB_PASSWORD", SharedPostgres::password);
		registry.add("MATRIZ_DEMO_DATA", () -> "false");
		registry.add("spring.datasource.hikari.maximum-pool-size", () -> "2");
	}

}
