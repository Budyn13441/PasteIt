package com.pdwww.pasteit.backend.api.exception;

public class StashReadOnlyException extends RuntimeException {
	public StashReadOnlyException() {
		super("Stash is read-only and cannot be modified");
	}
}
