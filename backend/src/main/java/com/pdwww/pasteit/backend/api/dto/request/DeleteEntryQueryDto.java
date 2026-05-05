package com.pdwww.pasteit.backend.api.dto.request;

import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeleteEntryQueryDto(
		@NotBlank
		@Size(min = 1, max = 1024)
		@Pattern(regexp = ValidationPatterns.STASH_PATH_REGEX, message = "path must be a valid absolute path")
		String path
) {
}
