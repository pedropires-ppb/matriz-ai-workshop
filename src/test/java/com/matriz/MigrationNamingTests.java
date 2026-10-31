package com.matriz;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The numbering of the migrations (ADR-007 decisions 8 and 10). Schema migrations have whole
 * versions; a demonstration migration is a sub-version of the schema migration it was written for,
 * so the two never clash in Flyway's single history. No part starts with 0, because Flyway reads
 * 1.0 and 01 as 1.
 */
class MigrationNamingTests {

	private static final Pattern SCHEMA = Pattern.compile("V([1-9]\\d*)__[a-z0-9_]+\\.sql");

	private static final Pattern DEMONSTRATION = Pattern.compile("V([1-9]\\d*)_([1-9]\\d*)__[a-z0-9_]+\\.sql");

	private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	@Test
	void schemaMigrationsHaveWholeVersionsAndNoPlaceholders() throws IOException {
		Resource[] schema = resources("db/migration");
		assertThat(schema).isNotEmpty();
		for (Resource migration : schema) {
			assertThat(migration.getFilename()).matches(SCHEMA);
			assertThat(migration.getContentAsString(StandardCharsets.UTF_8)).as(migration.getFilename())
				.doesNotContain("${");
		}
	}

	@Test
	void demonstrationMigrationsAreSubVersionsOfAnExistingSchemaMigration() throws IOException {
		List<String> schemaVersions = new ArrayList<>();
		for (Resource migration : resources("db/migration")) {
			schemaVersions.add(version(SCHEMA, migration));
		}
		Resource[] demonstration = resources("db/demo");
		assertThat(demonstration).isNotEmpty();
		List<String> versions = new ArrayList<>(schemaVersions);
		for (Resource migration : demonstration) {
			assertThat(migration.getFilename()).matches(DEMONSTRATION);
			String version = version(DEMONSTRATION, migration);
			assertThat(schemaVersions).as(migration.getFilename()).contains(version.substring(0, version.indexOf('.')));
			versions.add(version);
		}
		assertThat(versions).doesNotHaveDuplicates();
	}

	private Resource[] resources(String location) throws IOException {
		return this.resolver.getResources("classpath*:" + location + "/*.sql");
	}

	private static String version(Pattern pattern, Resource migration) {
		Matcher matcher = pattern.matcher(migration.getFilename());
		assertThat(matcher.matches()).as(migration.getFilename()).isTrue();
		return (matcher.groupCount() == 1) ? matcher.group(1) : matcher.group(1) + "." + matcher.group(2);
	}

}
