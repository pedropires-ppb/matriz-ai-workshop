package com.matriz.projects.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.matriz.identity.PersonDirectory;
import com.matriz.identity.PersonId;
import com.matriz.projects.AccessDecision;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectAccessPolicy;
import com.matriz.projects.ProjectAction;
import com.matriz.projects.ProjectId;
import com.matriz.projects.application.InvalidProjectException.InvalidField;
import com.matriz.projects.domain.Project;
import com.matriz.projects.domain.ProjectRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds a person to a project as a Project Participant (FR-004; US-002; Assumptions A-001, A-011, A-030).
 * The answers come in the order of ADR-004 decision 5: 404, 403, 422, 409. The project's row is locked
 * for the whole use case (ADR-006).
 */
@Service
public class AddParticipant {

	private final ProjectRepository projects;

	private final ProjectAccessPolicy policy;

	private final PersonDirectory people;

	AddParticipant(ProjectRepository projects, ProjectAccessPolicy policy, PersonDirectory people) {
		this.projects = projects;
		this.policy = policy;
		this.people = people;
	}

	/** Checks only the 404 and the 403, before the content is read (ADR-004 decision 5). */
	@Transactional(readOnly = true)
	public void checkCanAdd(PersonId person, ProjectId id) {
		Project project = this.projects.findById(id).orElseThrow(ProjectNotFoundException::new);
		ProjectRules.requireMemberWithPermission(
				this.policy.decide(ProjectAction.MANAGE_MEMBERS, project.state(), project.roleOf(person)));
	}

	/**
	 * @param added the identifier of the person to add, as sent
	 * @throws ProjectNotFoundException if there is no such project, or the person is not a member
	 * @throws NotProjectOwnerException if the person is a Project Participant
	 * @throws InvalidProjectException if no person has that identifier
	 * @throws ProjectCompletedException if the project is Completed
	 * @throws MembershipConflictException if the person to add is already a member
	 */
	@Transactional
	public MemberDetails add(PersonId person, ProjectId id, String added) {
		Project project = this.projects.findByIdForUpdate(id).orElseThrow(ProjectNotFoundException::new);
		AccessDecision decision = this.policy.decide(ProjectAction.MANAGE_MEMBERS, project.state(),
				project.roleOf(person));
		ProjectRules.requireMemberWithPermission(decision);
		PersonId named = personId(added);
		Map<PersonId, String> names = named == null ? Map.of() : this.people.displayNames(List.of(named));
		if (named == null || !names.containsKey(named)) {
			throw new InvalidProjectException("The person to add was not found.",
					List.of(new InvalidField("/personId", "No person has this identifier.")));
		}
		ProjectRules.requireNoConflict(decision);
		if (project.roleOf(named).isPresent()) {
			throw MembershipConflictException.alreadyMember();
		}
		project.addParticipant(person, named);
		return new MemberDetails(named, names.get(named), MemberRole.PROJECT_PARTICIPANT, false);
	}

	/** The identifier, or {@code null} if it is not a canonical UUID and so names nobody. */
	static PersonId personId(String text) {
		try {
			UUID uuid = UUID.fromString(text);
			return uuid.toString().equalsIgnoreCase(text) ? new PersonId(uuid) : null;
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

}
