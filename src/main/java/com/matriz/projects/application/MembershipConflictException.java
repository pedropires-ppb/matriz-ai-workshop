package com.matriz.projects.application;

/** The membership cannot change as asked; nothing changes: 409 (ADR-004; openapi.yaml, Members). */
public class MembershipConflictException extends RuntimeException {

	private MembershipConflictException(String detail) {
		super(detail);
	}

	static MembershipConflictException alreadyMember() {
		return new MembershipConflictException("The person is already a member of the project.");
	}

	static MembershipConflictException projectOwner() {
		return new MembershipConflictException("The Project Owner cannot be removed from the project.");
	}

	/** EX-007; Assumption A-009. */
	static MembershipConflictException assignedTasks() {
		return new MembershipConflictException(
				"The person is assigned to tasks of this project; remove those assignments first.");
	}

}
