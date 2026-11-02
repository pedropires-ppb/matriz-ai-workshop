package com.matriz.identity.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * The access tokens ended by signing out, kept until they expire (ADR-005, open point "How
 * sign-out is enforced"). Technical data of Identity, not an aggregate (ADR-008 decision 2): a
 * token is known only by its identifier, its {@code jti} claim.
 */
public interface EndedAccessTokens {

	/** Records that the token has ended; recording it again changes nothing. */
	void end(UUID tokenId, Instant expiresAt);

	boolean isEnded(UUID tokenId);

	/** Forgets the ended tokens that expired before the given time, and returns how many. */
	int deleteExpiredBefore(Instant time);

}
