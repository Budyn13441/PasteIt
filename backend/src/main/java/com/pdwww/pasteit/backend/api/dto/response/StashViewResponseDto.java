package com.pdwww.pasteit.backend.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record StashViewResponseDto(
		@NotNull
		@JsonProperty("valid_until")
		OffsetDateTime validUntil,
		@NotNull
		@JsonProperty("is_read_only")
		Boolean isReadOnly,
		@Valid
		@NotNull
		@JsonProperty("file_tree")
		FileTreeNodeViewDto fileTree
) {
}
