package com.pdwww.pasteit.backend.api.dto.request;

import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DownloadEntryQueryDto {

	@NotBlank
	@Size(min = 1, max = 1024)
	@Pattern(regexp = ValidationPatterns.STASH_PATH_REGEX, message = "path must be a valid absolute path")
	private String path;

	@Pattern(regexp = ValidationPatterns.DOWNLOAD_FORMAT_REGEX, message = "format can only be 'zip'")
	private String format;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}
}
