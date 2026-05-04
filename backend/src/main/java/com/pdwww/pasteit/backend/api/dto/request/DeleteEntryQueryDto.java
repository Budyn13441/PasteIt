package com.pdwww.pasteit.backend.api.dto.request;

import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DeleteEntryQueryDto {

	@NotBlank
	@Size(min = 1, max = 1024)
	@Pattern(regexp = ValidationPatterns.STASH_PATH_REGEX, message = "path must be a valid absolute path")
	private String path;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
