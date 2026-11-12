package com.matriz.projects.application;

import com.matriz.identity.PersonId;
import com.matriz.projects.MemberRole;

/**
 * A member as shown to another member (openapi.yaml, Member): the display name, never the login
 * name (Assumption A-017), and whether they are assigned to a task that is not deleted, which keeps
 * them from being removed (EX-007; Assumption A-009).
 */
public record MemberDetails(PersonId personId, String displayName, MemberRole role, boolean hasAssignedTasks) {

}
