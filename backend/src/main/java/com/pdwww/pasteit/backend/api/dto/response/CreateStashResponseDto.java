package com.pdwww.pasteit.backend.api.dto.response;

import jakarta.validation.constraints.NotBlank;

public class CreateStashResponseDto {

	@NotBlank
	private String code;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
