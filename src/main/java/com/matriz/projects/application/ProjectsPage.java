package com.matriz.projects.application;

import java.util.List;

/**
 * One page of My projects (openapi.yaml, ProjectPage).
 * @param page the number of the page, from 1
 * @param size the maximum number of projects in a page
 * @param totalItems the number of projects that match, across all pages
 * @param totalPages the number of pages; 0 if no project matches
 */
public record ProjectsPage(List<ProjectSummary> items, int page, int size, long totalItems, long totalPages) {

}
