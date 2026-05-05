package com.pdwww.pasteit.backend.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FileTreeNodeViewDto(
		@NotBlank
		String path,
		@NotBlank
		@Size(max = 255)
		String name,
		@NotNull
		@JsonProperty("is_directory")
		Boolean isDirectory,
		@Valid
		@JsonInclude(JsonInclude.Include.NON_NULL)
		FileContentViewDto file,
		@Valid
		@JsonInclude(JsonInclude.Include.NON_NULL)
		List<@Valid FileTreeNodeViewDto> children
) {
}
