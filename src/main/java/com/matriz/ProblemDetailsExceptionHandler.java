package com.matriz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Answers every error with Problem Details (ADR-004 decision 3). The detail texts of Spring's
 * exceptions are in messages.properties; the title is the HTTP status phrase and the type is
 * omitted, as in openapi.yaml.
 */
@RestControllerAdvice
class ProblemDetailsExceptionHandler extends ResponseEntityExceptionHandler {

	static final String UNEXPECTED_ERROR_DETAIL = "An unexpected error occurred.";

	private static final Logger log = LoggerFactory.getLogger(ProblemDetailsExceptionHandler.class);

	@ExceptionHandler(Exception.class)
	ResponseEntity<Object> handleUnexpectedException(Exception ex, WebRequest request) {
		log.error("Unexpected error", ex);
		ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				UNEXPECTED_ERROR_DETAIL);
		return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
	}

}
