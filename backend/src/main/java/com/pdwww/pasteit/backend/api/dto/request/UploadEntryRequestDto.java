package com.pdwww.pasteit.backend.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;
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
		@Pattern(regexp = ValidationPatterns.ABSOLUTE_PATH_REGEX, message = "parent_path must be empty or a valid absolute path")
		@JsonProperty("parent_path")
		String parent_path,
		@NotNull
		@Size(max = 255)
		@JsonProperty("name")
		String name,
		@NotNull
		MultipartFile data
) {
}
