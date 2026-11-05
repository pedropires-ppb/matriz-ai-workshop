package com.matriz.tasks.web;

import java.io.IOException;
import java.net.URI;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import com.matriz.identity.SignedInPerson;
import com.matriz.projects.ProjectId;
import com.matriz.tasks.TaskAction;
import com.matriz.tasks.TaskId;
import com.matriz.tasks.TaskPriority;
import com.matriz.tasks.TaskState;
import com.matriz.tasks.application.ChangeTaskState;
import com.matriz.tasks.application.CheckTaskAccess;
import com.matriz.tasks.application.CreateTask;
import com.matriz.tasks.application.DeleteTask;
import com.matriz.tasks.application.GetTask;
import com.matriz.tasks.application.ListTasks;
import com.matriz.tasks.application.TaskNotFoundException;
import com.matriz.tasks.application.UpdateTask;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Task management (openapi.yaml, {@code createTask}, {@code listTasks}, {@code getTask},
 * {@code updateTask}, {@code changeTaskState}, {@code deleteTask}; FR-006, FR-008, FR-009, FR-011,
 * FR-017, FR-023).
 * <ul>
 * <li>The project comes from the path, and the person from the token.</li>
 * <li>A person who is not a member is told the project, or the task, does not exist (ADR-004
 * decision 5).</li>
 * <li>The checks of a change come in the order {@code 404}, {@code 403}, {@code 415}, {@code 400},
 * {@code 422}, {@code 409} (ADR-004 decision 5), so a change reads its content by hand, after the
 * access check.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
class TaskController {

	static final MediaType MERGE_PATCH = MediaType.parseMediaType("application/merge-patch+json");

	static final String NOT_WELL_FORMED = "The request content is not well-formed JSON.";

	static final int DEFAULT_SIZE = 20;

	static final int MAXIMUM_SIZE = 100;

	private final CheckTaskAccess checkAccess;

	private final CreateTask createTask;

	private final ListTasks listTasks;

	private final GetTask getTask;

	private final UpdateTask updateTask;

	private final ChangeTaskState changeTaskState;

	private final DeleteTask deleteTask;

	private final JsonMapper jsonMapper;

	TaskController(CheckTaskAccess checkAccess, CreateTask createTask, ListTasks listTasks, GetTask getTask,
			UpdateTask updateTask, ChangeTaskState changeTaskState, DeleteTask deleteTask,
			JsonMapper jsonMapper) {
		this.checkAccess = checkAccess;
		this.createTask = createTask;
		this.listTasks = listTasks;
		this.getTask = getTask;
		this.updateTask = updateTask;
		this.changeTaskState = changeTaskState;
		this.deleteTask = deleteTask;
		this.jsonMapper = jsonMapper;
	}

	@PostMapping
	ResponseEntity<TaskResponse> create(SignedInPerson person, @PathVariable String projectId,
			HttpServletRequest request) throws Exception {
		ProjectId project = projectId(projectId);
		this.checkAccess.check(person.personId(), project, null, TaskAction.CREATE);
		requireMediaType(request, MediaType.APPLICATION_JSON);
		TaskContent.Parsed parsed = new TaskContent(readContent(request)).forCreate();
		TaskResponse created = TaskResponse.of(this.createTask.create(person.personId(), project, parsed.title(),
				parsed.description(), parsed.priority()));
		return ResponseEntity.created(URI.create("/api/projects/" + created.projectId() + "/tasks/" + created.taskId()))
			.body(created);
	}

	@GetMapping
	TaskResponse.Page list(SignedInPerson person, @PathVariable String projectId,
			@RequestParam(name = "page", required = false) String page,
			@RequestParam(name = "size", required = false) String size, HttpServletRequest request) {
		ProjectId project = projectId(projectId);
		this.checkAccess.check(person.personId(), project, null, TaskAction.VIEW);
		Set<TaskState> states = codes(request, "state", TaskState.class, TaskState::valueOf,
				"TO_DO, IN_PROGRESS, or DONE");
		Set<TaskPriority> priorities = codes(request, "priority", TaskPriority.class, TaskPriority::valueOf,
				"LOW, MEDIUM, or HIGH");
		int pageNumber = integer("page", page, 1, 1, Integer.MAX_VALUE, "an integer of at least 1");
		int pageSize = integer("size", size, DEFAULT_SIZE, 1, MAXIMUM_SIZE, "an integer from 1 to 100");
		return TaskResponse.Page
			.of(this.listTasks.list(person.personId(), project, states, priorities, pageNumber, pageSize));
	}

	@GetMapping("/{taskId}")
	TaskResponse get(SignedInPerson person, @PathVariable String projectId, @PathVariable String taskId) {
		return TaskResponse.of(this.getTask.get(person.personId(), projectId(projectId), taskId(taskId)));
	}

	@PatchMapping("/{taskId}")
	TaskResponse update(SignedInPerson person, @PathVariable String projectId, @PathVariable String taskId,
			HttpServletRequest request) throws Exception {
		ProjectId project = projectId(projectId);
		TaskId task = taskId(taskId);
		this.checkAccess.check(person.personId(), project, task, TaskAction.EDIT);
		requireMediaType(request, MERGE_PATCH);
		TaskContent.Parsed parsed = new TaskContent(readContent(request)).forUpdate();
		return TaskResponse.of(this.updateTask.update(person.personId(), project, task, parsed.title(),
				parsed.hasDescription(), parsed.description(), parsed.priority()));
	}

	@PutMapping("/{taskId}/state")
	TaskResponse changeState(SignedInPerson person, @PathVariable String projectId, @PathVariable String taskId,
			HttpServletRequest request) throws Exception {
		ProjectId project = projectId(projectId);
		TaskId task = taskId(taskId);
		this.checkAccess.check(person.personId(), project, task, TaskAction.CHANGE_STATE);
		requireMediaType(request, MediaType.APPLICATION_JSON);
		String state = new TaskContent(readContent(request)).forState();
		return TaskResponse.of(this.changeTaskState.change(person.personId(), project, task, state));
	}

	@DeleteMapping("/{taskId}")
	ResponseEntity<Void> delete(SignedInPerson person, @PathVariable String projectId, @PathVariable String taskId) {
		this.deleteTask.delete(person.personId(), projectId(projectId), taskId(taskId));
		return ResponseEntity.noContent().build();
	}

	/** An identifier that is not a canonical UUID names nothing: 404, not 400. */
	private static ProjectId projectId(String text) {
		return new ProjectId(uuid(text, TaskNotFoundException.ofProject()));
	}

	private static TaskId taskId(String text) {
		return new TaskId(uuid(text, TaskNotFoundException.ofTask()));
	}

	private static UUID uuid(String text, TaskNotFoundException notFound) {
		try {
			UUID uuid = UUID.fromString(text);
			if (!uuid.toString().equalsIgnoreCase(text)) {
				throw notFound;
			}
			return uuid;
		}
		catch (IllegalArgumentException ex) {
			throw notFound;
		}
	}

	private static void requireMediaType(HttpServletRequest request, MediaType required)
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
		if (type == null || !required.isCompatibleWith(type)) {
			throw new HttpMediaTypeNotSupportedException(type, List.of(required));
		}
	}

	private JsonNode readContent(HttpServletRequest request) throws IOException {
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

	/** The values of a repeatable filter, which must all be codes; absent means no restriction. */
	private static <E extends Enum<E>> Set<E> codes(HttpServletRequest request, String name, Class<E> type,
			Function<String, E> parse, String expected) {
		String[] values = request.getParameterValues(name);
		Set<E> codes = EnumSet.noneOf(type);
		if (values == null) {
			return codes;
		}
		for (String value : values) {
			try {
				codes.add(parse.apply(value));
			}
			catch (IllegalArgumentException ex) {
				throw badRequest("The query parameter " + name + " must be " + expected + ".");
			}
		}
		return codes;
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
