package com.pdwww.pasteit.backend.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RenameEntryRequestDto(
		@NotBlank
		@Size(min = 1, max = 1024)
		@Pattern(regexp = ValidationPatterns.ABSOLUTE_PATH_REGEX, message = "old_path must be a valid absolute path")
		@JsonProperty("old_path")
		String oldPath,
		@NotBlank
		@Size(max = 255)
		@Pattern(regexp = ValidationPatterns.ABSOLUTE_PATH_REGEX, message = "new_name must not contain path separators")
		@JsonProperty("new_name")
		String newName
) {
}
