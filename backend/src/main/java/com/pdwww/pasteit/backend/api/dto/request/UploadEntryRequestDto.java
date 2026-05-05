package com.pdwww.pasteit.backend.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record UploadEntryRequestDto(
		@NotNull
		@JsonProperty("is_directory")
		Boolean is_directory,
		@NotNull
		@Size(max = 1024)
		@Pattern(regexp = ValidationPatterns.STASH_PATH_OR_EMPTY_REGEX, message = "parent_path must be empty or a valid absolute path")
		@JsonProperty("parent_path")
		String parent_path,
		@NotBlank
		@Size(max = 255)
		@Pattern(regexp = ValidationPatterns.ENTRY_NAME_REGEX, message = "name must not contain path separators")
		String name,
		@NotNull
		MultipartFile data
) {

	public UploadEntryRequestDto {
		parent_path = parent_path == null ? "" : parent_path;
	}

	@AssertTrue(message = "When is_directory is true, name must not have a file extension")
	public boolean isDirectoryNameValid() {
		if (is_directory == null || name == null || name.isBlank()) {
			return true;
		}
		return !is_directory || !name.matches(ValidationPatterns.FILE_NAME_WITH_EXTENSION_REGEX);
	}

	@AssertTrue(message = "When is_directory is false, name must have a file extension")
	public boolean isFileNameValid() {
		if (is_directory == null || name == null || name.isBlank()) {
			return true;
		}
		return is_directory || name.matches(ValidationPatterns.FILE_NAME_WITH_EXTENSION_REGEX);
	}
}
