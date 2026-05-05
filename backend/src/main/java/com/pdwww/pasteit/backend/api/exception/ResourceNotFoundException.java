package com.pdwww.pasteit.backend.api.exception;

public class ResourceNotFoundException extends RuntimeException {
	public ResourceNotFoundException(String path) {
		super("Resource not found at path: " + path);
	}
}
