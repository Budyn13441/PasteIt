package com.pdwww.pasteit.backend.api.dto.response;

import jakarta.validation.constraints.NotBlank;

public record CreateStashResponseDto(
		@NotBlank
		String code
) {
}
