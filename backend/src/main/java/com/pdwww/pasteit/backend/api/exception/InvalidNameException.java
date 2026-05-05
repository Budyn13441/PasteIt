package com.pdwww.pasteit.backend.api.exception;

public class InvalidNameException extends RuntimeException {
	public InvalidNameException(String name) {
		super("Invalid name format: " + name);
	}

	public InvalidNameException(String name, String reason) {
		super("Invalid name format: " + name + ". Reason: " + reason);
	}
}
