package com.matriz.tasks.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The server's clock, which gives the date and time of a comment (Assumption A-036). It is a bean so
 * that the tests can fix the time; the date and time never come from a client.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
