package com.pdwww.pasteit.backend.api.exception;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
	private int status;
	private String message;
	private String path;
	private LocalDateTime timestamp;
	private String error;
	private String details;

	public ErrorResponse(int status, String message, String path, String error) {
		this.status = status;
		this.message = message;
		this.path = path;
		this.error = error;
		this.timestamp = LocalDateTime.now();
	}

	public ErrorResponse(int status, String message, String path, String error, String details) {
		this(status, message, path, error);
		this.details = details;
	}

	// Getters
	public int getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public String getPath() {
		return path;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public String getError() {
		return error;
	}

	public String getDetails() {
		return details;
	}
}
