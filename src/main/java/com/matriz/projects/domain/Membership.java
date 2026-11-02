package com.matriz.projects.domain;

import java.util.Objects;

import com.matriz.identity.PersonId;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectId;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A person's membership of a project, with a role (glossary: Member). An entity inside the
 * {@link Project} aggregate (ADR-008 decision 2); it is created and changed only through the
 * project. Two memberships are equal when they have the same identifier.
 */
@Entity
@Table(name = "membership")
public class Membership {

	@EmbeddedId
	private MembershipId id;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false)
	private MemberRole role;

	Membership(ProjectId projectId, PersonId personId, MemberRole role) {
		this.id = new MembershipId(projectId, personId);
		this.role = Objects.requireNonNull(role, "A role is required.");
	}

	/** For Jakarta Persistence only. */
	protected Membership() {
	}

	public PersonId personId() {
		return this.id.personId();
	}

	public MemberRole role() {
		return this.role;
	}

	@Override
	public boolean equals(Object other) {
		return this == other || (other instanceof Membership membership && Objects.equals(this.id, membership.id));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.id);
	}

	@Override
	public String toString() {
		return "Membership[" + this.id + ", " + this.role + "]";
	}

}
