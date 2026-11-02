package com.matriz.identity.application;

import java.time.Instant;
import java.util.UUID;

import com.matriz.identity.domain.EndedAccessTokens;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Signs a person out: the presented access token is no longer accepted, even after a restart
 * (FR-014; ADR-005 decision 3 and open point "How sign-out is enforced"). Other tokens of the
 * same person are not affected.
 */
@Service
public class SignOut {

	private final EndedAccessTokens endedTokens;

	SignOut(EndedAccessTokens endedTokens) {
		this.endedTokens = endedTokens;
	}

	/** Ends the token with this identifier, and forgets the ended tokens that have expired. */
	@Transactional
	public void signOut(UUID tokenId, Instant expiresAt) {
		this.endedTokens.end(tokenId, expiresAt);
		this.endedTokens.deleteExpiredBefore(Instant.now());
	}

}
