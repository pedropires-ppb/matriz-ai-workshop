package com.matriz.projects.application;

import java.util.List;

import com.matriz.identity.NamedPerson;
import com.matriz.identity.PersonDirectory;
import com.matriz.identity.PersonId;
import com.matriz.projects.ProjectAccessPolicy;
import com.matriz.projects.ProjectAction;
import com.matriz.projects.ProjectId;
import com.matriz.projects.domain.Membership;
import com.matriz.projects.domain.Project;
import com.matriz.projects.domain.ProjectRepository;
import com.matriz.projects.domain.Whitespace;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lists the people who are not members of a project, for its Project Owner to choose whom to add
 * (FR-004; US-002; Assumption A-030, decided by the team on 2026-11-09 and pending the client). It
 * also answers while the project is Completed. The search text is trimmed and matches a part of the
 * display name, ignoring letter case; accents are significant, as for the project search (A-024).
 */
@Service
public class ListEligibleParticipants {

	private final ProjectRepository projects;

	private final ProjectAccessPolicy policy;

	private final PersonDirectory people;

	ListEligibleParticipants(ProjectRepository projects, ProjectAccessPolicy policy, PersonDirectory people) {
		this.projects = projects;
		this.policy = policy;
		this.people = people;
	}

	/**
	 * @param nameContains the search text, or {@code null} for everyone; it is not blank
	 * @throws ProjectNotFoundException if there is no such project, or the person is not a member
	 * @throws NotProjectOwnerException if the person is a Project Participant
	 */
	@Transactional(readOnly = true)
	public List<NamedPerson> list(PersonId person, ProjectId id, String nameContains) {
		Project project = this.projects.findById(id).orElseThrow(ProjectNotFoundException::new);
		ProjectRules.requireMemberWithPermission(this.policy.decide(ProjectAction.LIST_ELIGIBLE_PARTICIPANTS,
				project.state(), project.roleOf(person)));
		List<PersonId> members = project.members().stream().map(Membership::personId).toList();
		return this.people.findOthers(members, nameContains == null ? null : Whitespace.trim(nameContains));
	}

}
