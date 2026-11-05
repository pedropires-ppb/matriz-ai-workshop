package com.matriz.tasks.web;

import com.matriz.tasks.application.InvalidTaskException;
import com.matriz.tasks.application.NotProjectOwnerException;
import com.matriz.tasks.application.ProjectCompletedException;
import com.matriz.tasks.application.TaskNotFoundException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Answers the refusals of the task use cases with Problem Details (ADR-004 decision 3; openapi.yaml,
 * ProjectNotFound, TaskNotFound, NotProjectOwner, ProjectCompleted, and ValidationProblem). It runs
 * before the general handler, which would answer them as unexpected errors.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class TaskProblems {

	@ExceptionHandler(TaskNotFoundException.class)
	ResponseEntity<ProblemDetail> notFound(TaskNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(NotProjectOwnerException.class)
	ResponseEntity<ProblemDetail> notOwner(NotProjectOwnerException ex) {
		return problem(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(ProjectCompletedException.class)
	ResponseEntity<ProblemDetail> completed(ProjectCompletedException ex) {
		return problem(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(InvalidTaskException.class)
	ResponseEntity<ProblemDetail> invalid(InvalidTaskException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
		problem.setProperty("errors", ex.fields());
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(problem);
	}

	private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}

}
