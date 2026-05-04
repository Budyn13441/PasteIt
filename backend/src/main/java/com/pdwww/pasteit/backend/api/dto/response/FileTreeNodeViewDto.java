package com.pdwww.pasteit.backend.api.dto.response;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class FileTreeNodeViewDto {

	@NotBlank
	private String path;

	@NotNull
	private Boolean isDirectory;

	@Valid
	private FileContentViewDto file;

	@Valid
	private List<@Valid FileTreeNodeViewDto> children;

	@AssertTrue(message = "file must be present for files and absent for directories")
	public boolean isFileFieldConsistent() {
		if (isDirectory == null) {
			return true;
		}
		return isDirectory ? file == null : file != null;
	}

	@AssertTrue(message = "children must be present only for directories")
	public boolean isChildrenFieldConsistent() {
		if (isDirectory == null) {
			return true;
		}
		return isDirectory || children == null || children.isEmpty();
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Boolean getIsDirectory() {
		return isDirectory;
	}

	public void setIsDirectory(Boolean isDirectory) {
		this.isDirectory = isDirectory;
	}

	public FileContentViewDto getFile() {
		return file;
	}

	public void setFile(FileContentViewDto file) {
		this.file = file;
	}

	public List<FileTreeNodeViewDto> getChildren() {
		return children;
	}

	public void setChildren(List<FileTreeNodeViewDto> children) {
		this.children = children;
	}
}
