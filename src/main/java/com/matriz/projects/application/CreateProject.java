package com.matriz.projects.application;

import java.util.List;

import com.matriz.identity.PersonId;
import com.matriz.projects.MemberRole;
import com.matriz.projects.ProjectId;
import com.matriz.projects.application.InvalidProjectException.InvalidField;
import com.matriz.projects.domain.Project;
import com.matriz.projects.domain.ProjectDescription;
import com.matriz.projects.domain.ProjectName;
import com.matriz.projects.domain.ProjectRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a project (FR-001; US-001). The signed-in person becomes its Project Owner and its first
 * member; the owner is never read from the request (ADR-009).
 */
@Service
public class CreateProject {

	private final ProjectRepository projects;

	CreateProject(ProjectRepository projects) {
		this.projects = projects;
	}

	/**
	 * Creates an Active project and stores it.
	 * @param description the text, or {@code null} for none; empty after trimming means none
	 * @throws InvalidProjectException if the name is empty or too long, or the description is too long
	 */
	@Transactional
	public ProjectDetails create(PersonId owner, String name, String description) {
		List<InvalidField> invalid = ProjectRules.noProblems();
		ProjectName validName = ProjectRules.name(name == null ? "" : name, invalid);
		ProjectDescription validDescription = ProjectRules.description(description, invalid);
		ProjectRules.requireNoProblems(invalid);
		Project project = this.projects.save(Project.create(ProjectId.generate(), validName, validDescription, owner));
		return ProjectDetails.of(project, MemberRole.PROJECT_OWNER);
	}

}
