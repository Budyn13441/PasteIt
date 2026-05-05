package com.pdwww.pasteit.backend.api.exception;

public class ResourceAlreadyExistsException extends RuntimeException {
	public ResourceAlreadyExistsException(String path) {
		super("Resource already exists at path: " + path);
	}
}
