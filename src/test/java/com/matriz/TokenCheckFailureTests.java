package com.matriz;

import com.matriz.identity.domain.EndedAccessTokens;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.matriz.Contract.assertProblem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * A token whose sign-out record cannot be read, for example because the database is unavailable,
 * is neither accepted nor called invalid: the request gets {@code 500}, which reveals nothing of
 * the cause (ADR-005, open point "How sign-out is enforced"; ADR-004 decision 5).
 */
@AutoConfigureMockMvc
class TokenCheckFailureTests extends DatabaseTestSupport {

	private static final String HIDDEN_CAUSE = "fictitious database failure";

	@MockitoBean
	private EndedAccessTokens endedTokens;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcClient jdbc;

	@Test
	void aTokenThatCannotBeCheckedIsNotAccepted() throws Exception {
		String token = TestAccess.accessToken(this.mvc, TestAccess.addPerson(this.jdbc, "Rui Martins"));
		given(this.endedTokens.isEnded(any())).willThrow(new IllegalStateException(HIDDEN_CAUSE));

		MvcResult result = assertProblem(
				this.mvc.perform(get(TestAccess.ME).header(HttpHeaders.AUTHORIZATION, TestAccess.bearer(token))), 500,
				"Internal Server Error", "An unexpected error occurred.");

		assertThat(result.getResponse().getHeaders(HttpHeaders.WWW_AUTHENTICATE)).isEmpty();
		assertThat(result.getResponse().getContentAsString()).doesNotContain(HIDDEN_CAUSE);
	}

}
