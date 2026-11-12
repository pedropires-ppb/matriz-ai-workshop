package com.matriz.projects.web;

import com.matriz.projects.application.MemberDetails;

/**
 * A member of a project, their role, and whether they are assigned to a task (openapi.yaml, Member and
 * PersonSummary); {@link Person} is also a person who can be added.
 */
record MemberResponse(Person person, String role, boolean hasAssignedTasks) {

	static MemberResponse of(MemberDetails member) {
		return new MemberResponse(new Person(member.personId().toString(), member.displayName()),
				member.role().name(), member.hasAssignedTasks());
	}

	record Person(String personId, String displayName) {

	}

}
