package com.matriz.projects.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.matriz.identity.PersonDirectory;
import com.matriz.identity.PersonId;
import com.matriz.projects.AccessDecision;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectAccessPolicy;
import com.matriz.projects.ProjectAction;
import com.matriz.projects.ProjectId;
import com.matriz.projects.ProjectTasks;
import com.matriz.projects.domain.Membership;
import com.matriz.projects.domain.Project;
import com.matriz.projects.domain.ProjectRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shows the members of a project to any member, also when it is Completed (FR-025; US-026;
 * Assumptions A-027 and A-034). The Project Owner comes first, then the others by display name
 * ignoring letter case, then by identifier. Each says whether the member is assigned to a task (the
 * I6 decisions, question 2).
 */
@Service
public class ListProjectMembers {

	private static final Comparator<MemberDetails> ORDER = Comparator
		.comparing((MemberDetails member) -> member.role() != MemberRole.PROJECT_OWNER)
		.thenComparing(MemberDetails::displayName, String.CASE_INSENSITIVE_ORDER)
		.thenComparing((member) -> member.personId().value());

	private final ProjectRepository projects;

	private final ProjectAccessPolicy policy;

	private final PersonDirectory people;

	private final ProjectTasks tasks;

	ListProjectMembers(ProjectRepository projects, ProjectAccessPolicy policy, PersonDirectory people,
			ProjectTasks tasks) {
		this.projects = projects;
		this.policy = policy;
		this.people = people;
		this.tasks = tasks;
	}

	/** @throws ProjectNotFoundException if there is no such project, or the person is not a member */
	@Transactional(readOnly = true)
	public List<MemberDetails> list(PersonId person, ProjectId id) {
		Project project = this.projects.findById(id).orElseThrow(ProjectNotFoundException::new);
		AccessDecision decision = this.policy.decide(ProjectAction.VIEW, project.state(), project.roleOf(person));
		ProjectRules.requireMemberWithPermission(decision);
		Map<PersonId, String> names = this.people
			.displayNames(project.members().stream().map(Membership::personId).toList());
		Set<PersonId> assigned = this.tasks.assignedMembers(id);
		return project.members()
			.stream()
			.filter((member) -> names.containsKey(member.personId()))
			.map((member) -> new MemberDetails(member.personId(), names.get(member.personId()), member.role(),
					assigned.contains(member.personId())))
			.sorted(ORDER)
			.toList();
	}

}
