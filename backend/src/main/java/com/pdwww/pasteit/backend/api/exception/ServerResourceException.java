package com.pdwww.pasteit.backend.api.exception;

public class ServerResourceException extends RuntimeException {
	public ServerResourceException(String message) {
		super(message);
	}

	public ServerResourceException(String message, Throwable cause) {
		super(message, cause);
	}
}
