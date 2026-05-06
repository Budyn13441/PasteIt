package com.pdwww.pasteit.backend.api.dto.request;

import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

public record DownloadEntryQueryDto(
		@NotBlank
		@Size(min = 1, max = 1024)
		@Pattern(regexp = ValidationPatterns.ABSOLUTE_PATH_REGEX, message = "path must be a valid absolute path")
		String path,
		@Null
		@Pattern(regexp = ValidationPatterns.FORMAT_REGEX, message = "format can only be 'zip'")
		String format
) {
}
