package com.matriz.tasks.application;

import java.util.List;

/**
 * One page of the tasks of a project (openapi.yaml, TaskPage).
 * @param page the number of the page, from 1
 * @param size the maximum number of tasks in a page
 * @param totalItems the number of tasks that match, across all pages
 * @param totalPages the number of pages; 0 if no task matches
 */
public record TasksPage(List<TaskDetails> items, int page, int size, long totalItems, long totalPages) {

}
