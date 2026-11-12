package com.matriz.projects.application;

import com.matriz.identity.PersonId;
import com.matriz.projects.AccessDecision;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectAccessPolicy;
import com.matriz.projects.ProjectAction;
import com.matriz.projects.ProjectId;
import com.matriz.projects.ProjectTasks;
import com.matriz.projects.domain.Project;
import com.matriz.projects.domain.ProjectRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Removes a Project Participant from a project, who can then no longer view it (FR-005; US-009;
 * EX-007; Assumptions A-009, A-011). The answers come in the order of ADR-004 decision 5: 404, also
 * when the person to remove is not a member, then 403, then 409. The project's row is locked for the
 * whole use case, so the check for assigned tasks and the removal are one indivisible step: an
 * assignment made at the same time waits, and then finds the person no longer a member (ADR-006;
 * Assumption A-029).
 */
@Service
public class RemoveParticipant {

	private final ProjectRepository projects;

	private final ProjectAccessPolicy policy;

	private final ProjectTasks tasks;

	RemoveParticipant(ProjectRepository projects, ProjectAccessPolicy policy, ProjectTasks tasks) {
		this.projects = projects;
		this.policy = policy;
		this.tasks = tasks;
	}

	/**
	 * @param removed the identifier of the person to remove, as sent
	 * @throws MemberNotFoundException if there is no such project, the project is deleted, the person
	 * is not a member, or the person to remove is not a member
	 * @throws NotProjectOwnerException if the person is a Project Participant
	 * @throws ProjectCompletedException if the project is Completed
	 * @throws MembershipConflictException if the person to remove is the Project Owner, or is assigned
	 * to a task that is not deleted
	 */
	@Transactional
	public void remove(PersonId person, ProjectId id, String removed) {
		Project project = this.projects.findByIdForUpdate(id).orElseThrow(MemberNotFoundException::new);
		AccessDecision decision = this.policy.decide(ProjectAction.MANAGE_MEMBERS, project.state(),
				project.roleOf(person));
		if (decision == AccessDecision.NOT_FOUND) {
			throw new MemberNotFoundException();
		}
		PersonId named = AddParticipant.personId(removed);
		MemberRole role = named == null ? null : project.roleOf(named).orElse(null);
		if (role == null) {
			throw new MemberNotFoundException();
		}
		ProjectRules.requireMemberWithPermission(decision);
		ProjectRules.requireNoConflict(decision);
		if (role == MemberRole.PROJECT_OWNER) {
			throw MembershipConflictException.projectOwner();
		}
		boolean assigned = this.tasks.assignedMembers(id).contains(named);
		if (assigned) {
			throw MembershipConflictException.assignedTasks();
		}
		project.removeParticipant(person, named, assigned);
	}

}
