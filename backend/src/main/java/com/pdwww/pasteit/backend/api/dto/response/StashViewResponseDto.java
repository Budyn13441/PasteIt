package com.pdwww.pasteit.backend.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public class StashViewResponseDto {

	@NotNull
	@JsonProperty("valid_until")
	private OffsetDateTime validUntil;

	@NotNull
	@JsonProperty("is_read_only")
	private Boolean isReadOnly;

	@Valid
	@NotNull
	@JsonProperty("file_tree")
	private FileTreeNodeViewDto fileTree;

	public OffsetDateTime getValidUntil() {
		return validUntil;
	}

	public void setValidUntil(OffsetDateTime validUntil) {
		this.validUntil = validUntil;
	}

	public Boolean getIsReadOnly() {
		return isReadOnly;
	}

	public void setIsReadOnly(Boolean readOnly) {
		isReadOnly = readOnly;
	}

	public FileTreeNodeViewDto getFileTree() {
		return fileTree;
	}

	public void setFileTree(FileTreeNodeViewDto fileTree) {
		this.fileTree = fileTree;
	}
}
