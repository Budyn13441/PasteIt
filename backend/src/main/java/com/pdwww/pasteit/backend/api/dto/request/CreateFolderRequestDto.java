package com.pdwww.pasteit.backend.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateFolderRequestDto {

	@NotNull
	@Size(max = 1024)
	@Pattern(regexp = ValidationPatterns.STASH_PATH_OR_EMPTY_REGEX, message = "parent_path must be empty or a valid absolute path")
	@JsonProperty("parent_path")
	private String parentPath = "";

	@NotBlank
	@Size(max = 255)
	@Pattern(regexp = ValidationPatterns.ENTRY_NAME_REGEX, message = "name must not contain path separators")
	private String name;

	@AssertTrue(message = "name must not include a file extension for folders")
	public boolean isFolderNameValid() {
		if (name == null || name.isBlank()) {
			return true;
		}
		return !name.matches(ValidationPatterns.FILE_NAME_WITH_EXTENSION_REGEX);
	}

	public String getParentPath() {
		return parentPath;
	}

	public void setParentPath(String parentPath) {
		this.parentPath = parentPath;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
