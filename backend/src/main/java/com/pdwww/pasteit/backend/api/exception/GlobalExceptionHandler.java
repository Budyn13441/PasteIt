package com.pdwww.pasteit.backend.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import jakarta.validation.ConstraintViolationException;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex,
			Object body,
			HttpHeaders headers,
			HttpStatusCode statusCode,
			WebRequest request) {
		ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
		if (response != null && response.getBody() instanceof ProblemDetail problemDetail) {
			// Temporary for rapid development: expose full framework exception message.
			problemDetail.setProperty("details", fullExceptionMessage(ex));
		}
		return response;
	}

	@ExceptionHandler(StashNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleStashNotFound(StashNotFoundException ex, WebRequest request) {
		ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.NOT_FOUND.value(),
				ex.getMessage(),
				request.getDescription(false).replace("uri=", ""),
				"NOT_FOUND",
				fullExceptionMessage(ex)
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.contentType(MediaType.APPLICATION_JSON)
				.body(errorResponse);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
		ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.NOT_FOUND.value(),
				ex.getMessage(),
				request.getDescription(false).replace("uri=", ""),
				"NOT_FOUND",
				fullExceptionMessage(ex)
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.contentType(MediaType.APPLICATION_JSON)
				.body(errorResponse);
	}

	/**
	 * Handle 403 - Read-only access denied
	 */
	@ExceptionHandler(StashReadOnlyException.class)
	public ResponseEntity<ErrorResponse> handleStashReadOnly(StashReadOnlyException ex, WebRequest request) {
		ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.FORBIDDEN.value(),
				ex.getMessage(),
				request.getDescription(false).replace("uri=", ""),
				"FORBIDDEN",
				fullExceptionMessage(ex)
		);
		return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
	}

	/**
	 * Handle 409 - Conflict (resource already exists or already read-only)
	 */
	@ExceptionHandler(ResourceAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleResourceAlreadyExists(ResourceAlreadyExistsException ex, WebRequest request) {
		ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.CONFLICT.value(),
				ex.getMessage(),
				request.getDescription(false).replace("uri=", ""),
				"CONFLICT",
				fullExceptionMessage(ex)
		);
		return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
	}

	/**
	 * Handle 409 - Conflict (stash already read-only)
	 */
	@ExceptionHandler(StashAlreadyReadOnlyException.class)
	public ResponseEntity<ErrorResponse> handleStashAlreadyReadOnly(StashAlreadyReadOnlyException ex, WebRequest request) {
		ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.CONFLICT.value(),
				ex.getMessage(),
				request.getDescription(false).replace("uri=", ""),
				"CONFLICT",
				fullExceptionMessage(ex)
		);
		return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
	}

	/**
	 * Handle 422 - Invalid path/name format
	 */
	@ExceptionHandler(InvalidPathException.class)
	public ResponseEntity<ErrorResponse> handleInvalidPath(InvalidPathException ex, WebRequest request) {
		ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.UNPROCESSABLE_CONTENT.value(),
				ex.getMessage(),
				request.getDescription(false).replace("uri=", ""),
				"INVALID_PATH",
				fullExceptionMessage(ex)
		);
		return new ResponseEntity<>(errorResponse, HttpStatus.UNPROCESSABLE_CONTENT);
	}

	/**
	 * Handle 422 - Invalid name format
	 */
	@ExceptionHandler(InvalidNameException.class)
	public ResponseEntity<ErrorResponse> handleInvalidName(InvalidNameException ex, WebRequest request) {
		ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.UNPROCESSABLE_CONTENT.value(),
				ex.getMessage(),
				request.getDescription(false).replace("uri=", ""),
				"INVALID_NAME",
				fullExceptionMessage(ex)
		);
		return new ResponseEntity<>(errorResponse, HttpStatus.UNPROCESSABLE_CONTENT);
	}

	/**
	 * Handle 400 - Invalid request (generic business logic error)
	 */
	@ExceptionHandler(InvalidRequestException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex, WebRequest request) {
		ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.BAD_REQUEST.value(),
				ex.getMessage(),
				request.getDescription(false).replace("uri=", ""),
				"INVALID_REQUEST",
				fullExceptionMessage(ex)
		);
		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handle 400 - Constraint validation errors (e.g., @Pattern, @Size, @NotBlank)
	 * Maps to 400 for invalid patterns, and would map to 422 for semantic validation
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
		ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.BAD_REQUEST.value(),
				"Validation failed",
				request.getDescription(false).replace("uri=", ""),
				"VALIDATION_ERROR",
				fullExceptionMessage(ex)
		);
		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handle 507 - Server resource exhaustion (out of memory, disk space, etc.)
	 */
	@ExceptionHandler(ServerResourceException.class)
	public ResponseEntity<ErrorResponse> handleServerResource(ServerResourceException ex, WebRequest request) {
		ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.INSUFFICIENT_STORAGE.value(),
				ex.getMessage(),
				request.getDescription(false).replace("uri=", ""),
				"SERVER_RESOURCE_UNAVAILABLE",
				fullExceptionMessage(ex)
		);
		return new ResponseEntity<>(errorResponse, HttpStatus.INSUFFICIENT_STORAGE);
	}

	/**
	 * Handle 500 - Generic internal server error
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
		ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"An unexpected error occurred",
				request.getDescription(false).replace("uri=", ""),
				"INTERNAL_SERVER_ERROR",
				fullExceptionMessage(ex)
		);
		return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private String fullExceptionMessage(Throwable throwable) {
		StringBuilder details = new StringBuilder();
		Throwable current = throwable;
		while (current != null) {
			if (!details.isEmpty()) {
				details.append(" | Caused by: ");
			}
			String message = current.getMessage();
			details.append(current.getClass().getSimpleName());
			if (message != null && !message.isBlank()) {
				details.append(": ").append(message);
			}
			current = current.getCause();
		}
		return details.toString();
	}
}
