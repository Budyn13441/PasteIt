package com.pdwww.pasteit.backend.api.exception;

public class InvalidPathException extends RuntimeException {
	public InvalidPathException(String path) {
		super("Invalid path format: " + path);
	}

	public InvalidPathException(String path, String reason) {
		super("Invalid path format: " + path + ". Reason: " + reason);
	}
}
