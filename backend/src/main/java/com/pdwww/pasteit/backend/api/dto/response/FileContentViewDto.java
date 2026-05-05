package com.pdwww.pasteit.backend.api.dto.response;

import com.pdwww.pasteit.backend.api.dto.common.FileCategoryDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record FileContentViewDto(
		@NotBlank
		@Size(max = 255)
		String name,
		@NotNull
		@PositiveOrZero
		Long size,
		@NotNull
		FileCategoryDto category
) {
}
