package com.pdwww.pasteit.backend.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFolderRequestDto(
		@NotNull
		@Size(max = 1024)
		@Pattern(regexp = ValidationPatterns.ABSOLUTE_PATH_REGEX, message = "parent_path must be empty or a valid absolute path")
		@JsonProperty("parent_path")
		String parentPath,
		@NotBlank
		@Size(max = 255)
		@Pattern(regexp = ValidationPatterns.FILE_NAME_REGEX, message = "name must not contain path separators")
		String name
) {

	public CreateFolderRequestDto {
		parentPath = parentPath == null ? "" : parentPath;
	}
}
