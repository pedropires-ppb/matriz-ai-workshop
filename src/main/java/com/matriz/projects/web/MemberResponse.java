package com.matriz.projects.web;

import com.matriz.projects.application.MemberDetails;

/** A member of a project and their role (openapi.yaml, Member and PersonSummary). */
record MemberResponse(Person person, String role) {

	static MemberResponse of(MemberDetails member) {
		return new MemberResponse(new Person(member.personId().toString(), member.displayName()),
				member.role().name());
	}

	record Person(String personId, String displayName) {

	}

}
