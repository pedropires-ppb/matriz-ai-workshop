package com.matriz.identity.infrastructure;

import java.time.Instant;
import java.util.UUID;

import com.matriz.identity.domain.EndedAccessTokens;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * The table {@code ended_access_token} (migration V2), with native SQL through Jakarta Persistence,
 * because recording a token twice relies on PostgreSQL's {@code ON CONFLICT DO NOTHING} (ADR-008
 * decision 2).
 */
@Repository
class JpaEndedAccessTokens implements EndedAccessTokens {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional
	public void end(UUID tokenId, Instant expiresAt) {
		this.entityManager
			.createNativeQuery("INSERT INTO ended_access_token (token_id, expires_at) VALUES (:tokenId, :expiresAt) "
					+ "ON CONFLICT (token_id) DO NOTHING")
			.setParameter("tokenId", tokenId)
			.setParameter("expiresAt", expiresAt)
			.executeUpdate();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isEnded(UUID tokenId) {
		Object found = this.entityManager
			.createNativeQuery("SELECT EXISTS (SELECT 1 FROM ended_access_token WHERE token_id = :tokenId)")
			.setParameter("tokenId", tokenId)
			.getSingleResult();
		return Boolean.TRUE.equals(found);
	}

	@Override
	@Transactional
	public int deleteExpiredBefore(Instant time) {
		return this.entityManager.createNativeQuery("DELETE FROM ended_access_token WHERE expires_at < :time")
			.setParameter("time", time)
			.executeUpdate();
	}

}
