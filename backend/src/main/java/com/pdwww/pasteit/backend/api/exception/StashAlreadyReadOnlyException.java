package com.pdwww.pasteit.backend.api.exception;

public class StashAlreadyReadOnlyException extends RuntimeException {
	public StashAlreadyReadOnlyException() {
		super("Stash is already read-only");
	}
}
