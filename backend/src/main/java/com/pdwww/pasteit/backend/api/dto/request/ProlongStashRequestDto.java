package com.pdwww.pasteit.backend.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

public record ProlongStashRequestDto(
		@NotNull
		@JsonProperty("new_expiration_date")
		ZonedDateTime newExpirationDate
) {
}
