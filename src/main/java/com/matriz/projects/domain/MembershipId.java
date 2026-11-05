package com.matriz.projects.domain;

import java.io.Serializable;
import java.util.Objects;

import com.matriz.identity.PersonId;
import com.matriz.projects.ProjectId;

import jakarta.persistence.Embeddable;

/** Identifies a membership: one person in one project (ADR-008, mapping of Memberships). */
@Embeddable
public record MembershipId(ProjectId projectId, PersonId personId) implements Serializable {

	public MembershipId {
		Objects.requireNonNull(projectId, "A project identifier is required.");
		Objects.requireNonNull(personId, "A person identifier is required.");
	}

}
