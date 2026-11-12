package com.matriz.projects.web;

import com.matriz.projects.application.InvalidProjectException;
import com.matriz.projects.application.MemberNotFoundException;
import com.matriz.projects.application.MembershipConflictException;
import com.matriz.projects.application.NotProjectOwnerException;
import com.matriz.projects.application.ProjectCompletedException;
import com.matriz.projects.application.ProjectNotFoundException;
import com.matriz.projects.application.UnfinishedTasksException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Answers the refusals of the project use cases with Problem Details (ADR-004 decision 3;
 * openapi.yaml, ProjectNotFound, NotProjectOwner, ProjectCompleted, and ValidationProblem). It runs
 * before the general handler, which would answer them as unexpected errors.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ProjectProblems {

	@ExceptionHandler(ProjectNotFoundException.class)
	ResponseEntity<ProblemDetail> notFound(ProjectNotFoundException ex) {
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

	@ExceptionHandler(UnfinishedTasksException.class)
	ResponseEntity<ProblemDetail> unfinished(UnfinishedTasksException ex) {
		return problem(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(MemberNotFoundException.class)
	ResponseEntity<ProblemDetail> memberNotFound(MemberNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(MembershipConflictException.class)
	ResponseEntity<ProblemDetail> membershipConflict(MembershipConflictException ex) {
		return problem(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(InvalidProjectException.class)
	ResponseEntity<ProblemDetail> invalid(InvalidProjectException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
		problem.setProperty("errors", ex.fields());
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(problem);
	}

	private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}

}
