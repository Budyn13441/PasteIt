package com.pdwww.pasteit.backend.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public class ProlongStashRequestDto {

	@NotNull
	@JsonProperty("new_expiration_date")
	private OffsetDateTime newExpirationDate;

	public OffsetDateTime getNewExpirationDate() {
		return newExpirationDate;
	}

	public void setNewExpirationDate(OffsetDateTime newExpirationDate) {
		this.newExpirationDate = newExpirationDate;
	}
}
