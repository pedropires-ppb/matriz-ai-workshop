package com.matriz.tasks.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.matriz.tasks.application.InvalidTaskException;
import com.matriz.tasks.application.InvalidTaskException.InvalidField;

import tools.jackson.databind.JsonNode;

/**
 * The content of a request to create or change a task, checked against {@code CreateTaskRequest},
 * {@code UpdateTaskRequest}, and {@code ChangeTaskStateRequest} of openapi.yaml: only its shape. The
 * lengths of the texts and the codes are checked by the use cases, together, so that every invalid
 * member is named at once. Every well-formed content that is not a valid object receives {@code 422},
 * not {@code 400} (ADR-004 decision 5).
 */
final class TaskContent {

	static final String TITLE = "title";

	static final String DESCRIPTION = "description";

	static final String PRIORITY = "priority";

	static final String STATE = "state";

	static final String PERSON_ID = "personId";

	private static final String CODES_OF_PRIORITY = "Use one of LOW, MEDIUM, or HIGH.";

	private static final String CODES_OF_STATE = "Must be TO_DO, IN_PROGRESS, or DONE.";

	private final JsonNode content;

	TaskContent(JsonNode content) {
		this.content = content;
	}

	/**
	 * What to create a task with: {@code title} is required; {@code description} and {@code priority}
	 * are optional.
	 * @throws InvalidTaskException (422) if the content is not an object, a member has the wrong type,
	 * {@code title} is missing, or another property is present
	 */
	Parsed forCreate() {
		requireObject();
		List<InvalidField> invalid = new ArrayList<>();
		String title = title(true, invalid);
		String description = description(invalid);
		String priority = priority(invalid);
		noOtherThan(Set.of(TITLE, DESCRIPTION, PRIORITY), invalid);
		return result(invalid, new Parsed(title, hasMember(DESCRIPTION), description, priority));
	}

	/**
	 * The change to make: each of {@code title}, {@code description}, and {@code priority} is optional,
	 * one that is absent is left unchanged, and at least one is required.
	 * @throws InvalidTaskException (422) if the content is not an object or is an empty one, a member
	 * has the wrong type, {@code title} or {@code priority} is {@code null}, or another property is
	 * present
	 */
	Parsed forUpdate() {
		requireObject();
		List<InvalidField> invalid = new ArrayList<>();
		String title = title(false, invalid);
		String description = description(invalid);
		String priority = priority(invalid);
		noOtherThan(Set.of(TITLE, DESCRIPTION, PRIORITY), invalid);
		if (invalid.isEmpty() && !this.content.propertyNames().iterator().hasNext()) {
			invalid.add(new InvalidField("", "At least one of title, description, and priority is required."));
		}
		return result(invalid, new Parsed(title, hasMember(DESCRIPTION), description, priority));
	}

	/**
	 * The state to set: {@code state} is required.
	 * @throws InvalidTaskException (422) if the content is not an object, {@code state} is missing or
	 * is not a string, or another property is present
	 */
	String forState() {
		requireObject();
		List<InvalidField> invalid = new ArrayList<>();
		JsonNode node = this.content.get(STATE);
		String state = null;
		if (node != null && node.isString()) {
			state = node.stringValue();
		}
		else {
			invalid.add(new InvalidField(pointer(STATE), CODES_OF_STATE));
		}
		noOtherThan(Set.of(STATE), invalid);
		return result(invalid, state);
	}

	/**
	 * The person to assign: {@code personId} is required.
	 * @throws InvalidTaskException (422) if the content is not an object, {@code personId} is missing,
	 * empty, or not a string, or another property is present
	 */
	String forAssignee() {
		requireObject();
		List<InvalidField> invalid = new ArrayList<>();
		JsonNode node = this.content.get(PERSON_ID);
		String person = null;
		if (node != null && node.isString() && !node.stringValue().isEmpty()) {
			person = node.stringValue();
		}
		else {
			invalid.add(new InvalidField(pointer(PERSON_ID), "A person identifier is required."));
		}
		noOtherThan(Set.of(PERSON_ID), invalid);
		return result(invalid, person);
	}

	private void requireObject() {
		if (!this.content.isObject()) {
			throw new InvalidTaskException(List.of(new InvalidField("", "The content must be a JSON object.")));
		}
	}

	private boolean hasMember(String name) {
		return this.content.get(name) != null;
	}

	private String title(boolean required, List<InvalidField> invalid) {
		JsonNode node = this.content.get(TITLE);
		if (node == null) {
			if (required) {
				invalid.add(new InvalidField(pointer(TITLE), "A title is required."));
			}
			return null;
		}
		if (node.isNull()) {
			invalid.add(new InvalidField(pointer(TITLE),
					required ? "A title is required." : "The title cannot be removed."));
			return null;
		}
		if (!node.isString()) {
			invalid.add(new InvalidField(pointer(TITLE), "The title must be a string."));
			return null;
		}
		return node.stringValue();
	}

	private String description(List<InvalidField> invalid) {
		JsonNode node = this.content.get(DESCRIPTION);
		if (node == null || node.isNull()) {
			return null;
		}
		if (!node.isString()) {
			invalid.add(new InvalidField(pointer(DESCRIPTION), "The description must be a string or null."));
			return null;
		}
		return node.stringValue();
	}

	private String priority(List<InvalidField> invalid) {
		JsonNode node = this.content.get(PRIORITY);
		if (node == null) {
			return null;
		}
		if (!node.isString()) {
			invalid.add(new InvalidField(pointer(PRIORITY), CODES_OF_PRIORITY));
			return null;
		}
		return node.stringValue();
	}

	private void noOtherThan(Set<String> allowed, List<InvalidField> invalid) {
		for (String property : this.content.propertyNames()) {
			if (!allowed.contains(property)) {
				invalid.add(new InvalidField(pointer(property), "This property is not allowed."));
			}
		}
	}

	private static <T> T result(List<InvalidField> invalid, T parsed) {
		if (!invalid.isEmpty()) {
			throw new InvalidTaskException(invalid);
		}
		return parsed;
	}

	/** A JSON Pointer (RFC 6901) to a member of the content. */
	private static String pointer(String name) {
		return "/" + name.replace("~", "~0").replace("/", "~1");
	}

	/**
	 * What the content says.
	 * @param title the title, or {@code null} if absent
	 * @param hasDescription whether {@code description} is present, even as {@code null}
	 * @param description the description, or {@code null}
	 * @param priority the code of the priority, or {@code null} if absent
	 */
	record Parsed(String title, boolean hasDescription, String description, String priority) {

	}

}
