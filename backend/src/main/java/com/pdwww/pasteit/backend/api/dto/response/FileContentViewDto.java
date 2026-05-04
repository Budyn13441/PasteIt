package com.pdwww.pasteit.backend.api.dto.response;

import com.pdwww.pasteit.backend.api.dto.common.FileCategoryDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class FileContentViewDto {

	@NotBlank
	@Size(max = 255)
	private String name;

	@NotNull
	@PositiveOrZero
	private Long size;

	@NotNull
	private FileCategoryDto category;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public FileCategoryDto getCategory() {
		return category;
	}

	public void setCategory(FileCategoryDto category) {
		this.category = category;
	}
}
