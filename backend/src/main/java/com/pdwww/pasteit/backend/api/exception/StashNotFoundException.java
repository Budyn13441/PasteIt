package com.pdwww.pasteit.backend.api.exception;

public class StashNotFoundException extends RuntimeException {
	public StashNotFoundException(String code) {
		super("Stash with code '" + code + "' not found");
	}
}
