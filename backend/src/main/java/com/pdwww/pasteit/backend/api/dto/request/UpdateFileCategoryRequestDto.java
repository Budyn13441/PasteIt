package com.pdwww.pasteit.backend.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pdwww.pasteit.backend.api.dto.common.FileCategoryDto;
import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateFileCategoryRequestDto(
		@NotBlank
		@Size(min = 1, max = 1024)
		@Pattern(regexp = ValidationPatterns.ABSOLUTE_PATH_REGEX, message = "file_path must be a valid absolute path")
		@JsonProperty("file_path")
		String filePath,
		@NotNull
		FileCategoryDto category
) {
}
