package com.pdwww.pasteit.backend.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class UploadEntryRequestDto {

	@NotNull
	@JsonProperty("is_directory")
	private Boolean isDirectory;

	@NotNull
	@Size(max = 1024)
	@Pattern(regexp = ValidationPatterns.STASH_PATH_OR_EMPTY_REGEX, message = "parent_path must be empty or a valid absolute path")
	@JsonProperty("parent_path")
	private String parentPath = "";

	@NotBlank
	@Size(max = 255)
	@Pattern(regexp = ValidationPatterns.ENTRY_NAME_REGEX, message = "name must not contain path separators")
	private String name;

	@NotNull
	private MultipartFile data;

	@AssertTrue(message = "When is_directory is true, name must not have a file extension")
	public boolean isDirectoryNameValid() {
		if (isDirectory == null || name == null || name.isBlank()) {
			return true;
		}
		return !isDirectory || !name.matches(ValidationPatterns.FILE_NAME_WITH_EXTENSION_REGEX);
	}

	@AssertTrue(message = "When is_directory is false, name must have a file extension")
	public boolean isFileNameValid() {
		if (isDirectory == null || name == null || name.isBlank()) {
			return true;
		}
		return isDirectory || name.matches(ValidationPatterns.FILE_NAME_WITH_EXTENSION_REGEX);
	}

	public Boolean getIsDirectory() {
		return isDirectory;
	}

	public void setIsDirectory(Boolean isDirectory) {
		this.isDirectory = isDirectory;
	}

	public void setIs_directory(Boolean isDirectory) {
		this.isDirectory = isDirectory;
	}

	public String getParentPath() {
		return parentPath;
	}

	public void setParentPath(String parentPath) {
		this.parentPath = parentPath;
	}

	public void setParent_path(String parentPath) {
		this.parentPath = parentPath;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MultipartFile getData() {
		return data;
	}

	public void setData(MultipartFile data) {
		this.data = data;
	}
}
