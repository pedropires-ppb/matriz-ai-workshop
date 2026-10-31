package com.matriz;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The health check reports the application and its database as up, without details (ADR-007
 * decision 7). Only the health endpoint is exposed.
 */
@AutoConfigureMockMvc
class HealthCheckTests extends DatabaseTestSupport {

	@Autowired
	private MockMvc mvc;

	@Test
	void theApplicationAndItsDatabaseAreReportedAsUpWithoutDetails() throws Exception {
		this.mvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"))
			.andExpect(jsonPath("$.components.db.status").value("UP"))
			.andExpect(jsonPath("$..details").isEmpty());
	}

	@Test
	void noOtherActuatorEndpointIsExposed() throws Exception {
		this.mvc.perform(get("/actuator"))
			.andExpect(jsonPath("$._links.health").exists())
			.andExpect(jsonPath("$._links.env").doesNotExist())
			.andExpect(jsonPath("$._links.info").doesNotExist());
		this.mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
		this.mvc.perform(get("/actuator/info")).andExpect(status().isNotFound());
	}

}
