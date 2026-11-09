package com.matriz.projects.web;

import java.util.ArrayList;
import java.util.List;

import com.matriz.projects.application.InvalidProjectException;
import com.matriz.projects.application.InvalidProjectException.InvalidField;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.node.NullNode;

/**
 * The content of a request to create or edit a project, checked against {@code CreateProjectRequest}
 * and {@code UpdateProjectRequest} of openapi.yaml: only its shape. The lengths of the texts are
 * checked by the use cases. It reads any JSON value, including {@code null}, so that every
 * well-formed content that is not a valid object receives {@code 422}, not {@code 400}
 * (ADR-004 decision 5; Question 16 of the exercise).
 */
@JsonDeserialize(using = ProjectContent.Reader.class)
final class ProjectContent {

	static final String NAME = "name";

	static final String DESCRIPTION = "description";

	static final String STATE = "state";

	static final String COMPLETED = "COMPLETED";

	private final JsonNode content;

	ProjectContent(JsonNode content) {
		this.content = content;
	}

	/**
	 * The name and description to create a project with: {@code name} is required, and
	 * {@code description} is optional.
	 * @throws InvalidProjectException (422) if the content is not an object, a member has the wrong
	 * type, {@code name} is missing, or another property is present
	 */
	Parsed forCreate() {
		return parse(true);
	}

	/**
	 * The change to make: each of {@code name} and {@code description} is optional, and one that is
	 * absent is left unchanged.
	 * @throws InvalidProjectException (422) if the content is not an object, a member has the wrong
	 * type, {@code name} is {@code null}, or another property is present
	 */
	Parsed forUpdate() {
		return parse(false);
	}

	/**
	 * Checks the content of a request to complete the project ({@code CompleteProjectRequest}):
	 * an object whose only member is {@code state}, with the value {@code COMPLETED}. Reopening is not
	 * defined (OQ-006).
	 * @throws InvalidProjectException (422) otherwise
	 */
	void forCompletion() {
		List<InvalidField> invalid = new ArrayList<>();
		if (!this.content.isObject()) {
			invalid.add(new InvalidField("", "The content must be a JSON object."));
		}
		else {
			JsonNode state = this.content.get(STATE);
			if (state == null || !state.isString() || !COMPLETED.equals(state.stringValue())) {
				invalid.add(new InvalidField(pointer(STATE), "The only accepted value is COMPLETED."));
			}
			for (String property : this.content.propertyNames()) {
				if (!STATE.equals(property)) {
					invalid.add(new InvalidField(pointer(property), "This property is not allowed."));
				}
			}
		}
		if (!invalid.isEmpty()) {
			throw new InvalidProjectException("A project can only be changed to the state COMPLETED.", invalid);
		}
	}

	private Parsed parse(boolean nameRequired) {
		if (!this.content.isObject()) {
			throw new InvalidProjectException(
					List.of(new InvalidField("", "The content must be a JSON object.")));
		}
		List<InvalidField> invalid = new ArrayList<>();
		String name = null;
		JsonNode nameNode = this.content.get(NAME);
		if (nameNode == null) {
			if (nameRequired) {
				invalid.add(new InvalidField(pointer(NAME), "A name is required."));
			}
		}
		else if (nameNode.isNull()) {
			invalid.add(new InvalidField(pointer(NAME),
					nameRequired ? "A name is required." : "The name cannot be removed."));
		}
		else if (nameNode.isString()) {
			name = nameNode.stringValue();
		}
		else {
			invalid.add(new InvalidField(pointer(NAME), "The name must be a string."));
		}
		String description = null;
		JsonNode descriptionNode = this.content.get(DESCRIPTION);
		boolean hasDescription = descriptionNode != null;
		if (hasDescription && descriptionNode.isString()) {
			description = descriptionNode.stringValue();
		}
		else if (hasDescription && !descriptionNode.isNull()) {
			invalid.add(new InvalidField(pointer(DESCRIPTION), "The description must be a string or null."));
		}
		for (String property : this.content.propertyNames()) {
			if (!NAME.equals(property) && !DESCRIPTION.equals(property)) {
				invalid.add(new InvalidField(pointer(property), "This property is not allowed."));
			}
		}
		if (!invalid.isEmpty()) {
			throw new InvalidProjectException(invalid);
		}
		return new Parsed(name, hasDescription, description);
	}

	/** A JSON Pointer (RFC 6901) to a member of the content. */
	private static String pointer(String name) {
		return "/" + name.replace("~", "~0").replace("/", "~1");
	}

	/**
	 * What the content says.
	 * @param name the name, or {@code null} if absent
	 * @param hasDescription whether {@code description} is present, even as {@code null}
	 * @param description the description, or {@code null}
	 */
	record Parsed(String name, boolean hasDescription, String description) {

	}

	static final class Reader extends ValueDeserializer<ProjectContent> {

		@Override
		public ProjectContent deserialize(JsonParser parser, DeserializationContext context) {
			return new ProjectContent(context.readTree(parser));
		}

		@Override
		public ProjectContent getNullValue(DeserializationContext context) {
			return new ProjectContent(NullNode.getInstance());
		}

	}

}
