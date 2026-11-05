package com.matriz.projects.domain;

/**
 * A change to a project's name and description, as a JSON Merge Patch gives it (Assumption A-031).
 * @param name the new name, or {@code null} to leave it unchanged
 * @param changesDescription whether the description is given; if not, it is left unchanged
 * @param description the new description, or {@code null} to remove it; used only if
 * {@code changesDescription}
 */
public record ProjectEdit(ProjectName name, boolean changesDescription, ProjectDescription description) {

}
