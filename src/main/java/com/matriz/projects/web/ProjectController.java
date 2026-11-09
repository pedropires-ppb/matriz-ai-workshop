package com.matriz.projects.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.matriz.identity.SignedInPerson;
import com.matriz.projects.ProjectId;
import com.matriz.projects.application.CompleteProject;
import com.matriz.projects.application.CreateProject;
import com.matriz.projects.application.DeleteProject;
import com.matriz.projects.application.GetProject;
import com.matriz.projects.application.ListProjectMembers;
import com.matriz.projects.application.ListProjects;
import com.matriz.projects.application.ProjectNotFoundException;
import com.matriz.projects.application.UpdateProject;
import com.matriz.projects.domain.Whitespace;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Project management (openapi.yaml, {@code createProject}, {@code listProjects},
 * {@code getProject}, {@code updateProject}, {@code deleteProject}; FR-001, FR-003, FR-016, FR-022).
 * <ul>
 * <li>The Project Owner of a new project is the signed-in person, never a value in the request
 * (ADR-009 decision 2).</li>
 * <li>A person who is not a member is told the project does not exist (ADR-004 decision 5).</li>
 * <li>The checks of an edit come in the order {@code 404}, {@code 403}, {@code 415}, {@code 400},
 * {@code 422}, {@code 409} (ADR-004 decision 5), so an edit reads its content by hand, after the
 * access check.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/projects")
class ProjectController {

	static final MediaType MERGE_PATCH = MediaType.parseMediaType("application/merge-patch+json");

	static final String NOT_WELL_FORMED = "The request content is not well-formed JSON.";

	static final int DEFAULT_SIZE = 20;

	static final int MAXIMUM_SIZE = 100;

	private final CreateProject createProject;

	private final ListProjects listProjects;

	private final GetProject getProject;

	private final UpdateProject updateProject;

	private final DeleteProject deleteProject;

	private final ListProjectMembers listProjectMembers;

	private final CompleteProject completeProject;

	private final JsonMapper jsonMapper;

	ProjectController(CreateProject createProject, ListProjects listProjects, GetProject getProject,
			UpdateProject updateProject, DeleteProject deleteProject, ListProjectMembers listProjectMembers,
			CompleteProject completeProject, JsonMapper jsonMapper) {
		this.createProject = createProject;
		this.listProjects = listProjects;
		this.getProject = getProject;
		this.updateProject = updateProject;
		this.deleteProject = deleteProject;
		this.listProjectMembers = listProjectMembers;
		this.completeProject = completeProject;
		this.jsonMapper = jsonMapper;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<ProjectResponse> create(SignedInPerson person, @RequestBody ProjectContent content) {
		ProjectContent.Parsed parsed = content.forCreate();
		ProjectResponse created = ProjectResponse.of(
				this.createProject.create(person.personId(), parsed.name(), parsed.description()));
		return ResponseEntity.created(URI.create("/api/projects/" + created.projectId())).body(created);
	}

	@GetMapping
	ProjectPageResponse list(SignedInPerson person, @RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "page", required = false) String page,
			@RequestParam(name = "size", required = false) String size) {
		if (name != null && Whitespace.isBlank(name)) {
			throw badRequest("The query parameter name must not be empty.");
		}
		int pageNumber = integer("page", page, 1, 1, Integer.MAX_VALUE, "an integer of at least 1");
		int pageSize = integer("size", size, DEFAULT_SIZE, 1, MAXIMUM_SIZE, "an integer from 1 to 100");
		return ProjectPageResponse.of(this.listProjects.list(person.personId(), name, pageNumber, pageSize));
	}

	@GetMapping("/{projectId}")
	ProjectResponse get(SignedInPerson person, @PathVariable String projectId) {
		return ProjectResponse.of(this.getProject.get(person.personId(), id(projectId)));
	}

	@GetMapping("/{projectId}/members")
	List<MemberResponse> members(SignedInPerson person, @PathVariable String projectId) {
		return this.listProjectMembers.list(person.personId(), id(projectId))
			.stream()
			.map(MemberResponse::of)
			.toList();
	}

	@PatchMapping("/{projectId}")
	ProjectResponse update(SignedInPerson person, @PathVariable String projectId, HttpServletRequest request)
			throws Exception {
		ProjectId id = id(projectId);
		this.updateProject.checkCanEdit(person.personId(), id);
		requireMediaType(request, MERGE_PATCH);
		ProjectContent.Parsed parsed = new ProjectContent(readContent(request)).forUpdate();
		return ProjectResponse.of(this.updateProject.update(person.personId(), id, parsed.name(),
				parsed.hasDescription(), parsed.description()));
	}

	/** Completes the project: 404 and 403 first, then 415, 400, and 422, then 409 (ADR-004 decision 5). */
	@PutMapping("/{projectId}/state")
	ProjectResponse complete(SignedInPerson person, @PathVariable String projectId, HttpServletRequest request)
			throws Exception {
		ProjectId id = id(projectId);
		this.completeProject.checkCanComplete(person.personId(), id);
		requireMediaType(request, MediaType.APPLICATION_JSON);
		new ProjectContent(readContent(request)).forCompletion();
		return ProjectResponse.of(this.completeProject.complete(person.personId(), id));
	}

	@DeleteMapping("/{projectId}")
	ResponseEntity<Void> delete(SignedInPerson person, @PathVariable String projectId) {
		this.deleteProject.delete(person.personId(), id(projectId));
		return ResponseEntity.noContent().build();
	}

	/** An identifier that is not a canonical UUID names no project: 404, not 400. */
	private static ProjectId id(String text) {
		try {
			UUID uuid = UUID.fromString(text);
			if (!uuid.toString().equalsIgnoreCase(text)) {
				throw new ProjectNotFoundException();
			}
			return new ProjectId(uuid);
		}
		catch (IllegalArgumentException ex) {
			throw new ProjectNotFoundException();
		}
	}

	private static void requireMediaType(HttpServletRequest request, MediaType expected)
			throws HttpMediaTypeNotSupportedException {
		String header = request.getContentType();
		MediaType type = null;
		if (header != null) {
			try {
				type = MediaType.parseMediaType(header);
			}
			catch (RuntimeException ex) {
				type = null;
			}
		}
		if (type == null || !expected.isCompatibleWith(type)) {
			throw new HttpMediaTypeNotSupportedException(type, List.of(expected));
		}
	}

	private JsonNode readContent(HttpServletRequest request) throws java.io.IOException {
		JsonNode content;
		try {
			content = this.jsonMapper.readTree(request.getInputStream());
		}
		catch (JacksonException ex) {
			throw new HttpMessageNotReadableException(NOT_WELL_FORMED, new ServletServerHttpRequest(request));
		}
		if (content == null || content.isMissingNode()) {
			throw new HttpMessageNotReadableException(NOT_WELL_FORMED, new ServletServerHttpRequest(request));
		}
		return content;
	}

	private static int integer(String name, String text, int defaultValue, int minimum, int maximum,
			String expected) {
		if (text == null) {
			return defaultValue;
		}
		try {
			int value = Integer.parseInt(text);
			if (value >= minimum && value <= maximum) {
				return value;
			}
		}
		catch (NumberFormatException ex) {
			// Falls through to the same refusal as a number out of range.
		}
		throw badRequest("The query parameter " + name + " must be " + expected + ".");
	}

	private static ErrorResponseException badRequest(String detail) {
		return new ErrorResponseException(HttpStatus.BAD_REQUEST,
				ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail), null);
	}

}
