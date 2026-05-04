package com.pdwww.pasteit.backend.api.validation;

public final class ValidationPatterns {

	public static final String STASH_CODE_REGEX = "^[A-Za-z0-9_-]{1,128}$";
	public static final String STASH_PATH_REGEX = "^(/[a-zA-Z0-9._\\- ]+)+/?$";
	public static final String STASH_PATH_OR_EMPTY_REGEX = "^$|^(/[a-zA-Z0-9._\\- ]+)+/?$";
	public static final String ENTRY_NAME_REGEX = "^(?!\\.\\.?$)(?!.*[/\\\\]).{1,255}$";
	public static final String FILE_NAME_WITH_EXTENSION_REGEX = "^.+\\.[^./\\\\]+$";
	public static final String DOWNLOAD_FORMAT_REGEX = "^zip$";

	private ValidationPatterns() {
	}
}
