package com.pdwww.pasteit.backend.api.controller;

import com.pdwww.pasteit.backend.api.dto.request.CreateFolderRequestDto;
import com.pdwww.pasteit.backend.api.dto.request.DeleteEntryQueryDto;
import com.pdwww.pasteit.backend.api.dto.request.DownloadEntryQueryDto;
import com.pdwww.pasteit.backend.api.dto.request.RenameEntryRequestDto;
import com.pdwww.pasteit.backend.api.dto.request.UpdateFileCategoryRequestDto;
import com.pdwww.pasteit.backend.api.dto.request.UploadEntryRequestDto;
import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Validated
public class StashContentController {

	@PostMapping(value = "/upload/{code}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Void> uploadEntry(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.STASH_CODE_REGEX, message = "code contains unsupported characters") String code,
			@Valid @ModelAttribute UploadEntryRequestDto request) {
		throw new UnsupportedOperationException("Endpoint not implemented yet: POST /api/v1/upload/{code}");
	}

	@GetMapping(value = "/download/{code}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<Resource> downloadEntry(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.STASH_CODE_REGEX, message = "code contains unsupported characters") String code,
			@Valid @ModelAttribute DownloadEntryQueryDto query) {
		throw new UnsupportedOperationException("Endpoint not implemented yet: GET /api/v1/download/{code}");
	}

	@DeleteMapping("/delete/{code}")
	public ResponseEntity<Void> deleteEntry(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.STASH_CODE_REGEX, message = "code contains unsupported characters") String code,
			@Valid @ModelAttribute DeleteEntryQueryDto query) {
		throw new UnsupportedOperationException("Endpoint not implemented yet: DELETE /api/v1/delete/{code}");
	}

	@PostMapping("/rename/{code}")
	public ResponseEntity<Void> renameEntry(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.STASH_CODE_REGEX, message = "code contains unsupported characters") String code,
			@Valid @RequestBody RenameEntryRequestDto request) {
		throw new UnsupportedOperationException("Endpoint not implemented yet: POST /api/v1/rename/{code}");
	}

	@PostMapping("/new-folder/{code}")
	public ResponseEntity<Void> createFolder(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.STASH_CODE_REGEX, message = "code contains unsupported characters") String code,
			@Valid @RequestBody CreateFolderRequestDto request) {
		throw new UnsupportedOperationException("Endpoint not implemented yet: POST /api/v1/new-folder/{code}");
	}

	@PostMapping("/update-category/{code}")
	public ResponseEntity<Void> updateCategory(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.STASH_CODE_REGEX, message = "code contains unsupported characters") String code,
			@Valid @RequestBody UpdateFileCategoryRequestDto request) {
		throw new UnsupportedOperationException("Endpoint not implemented yet: POST /api/v1/update-category/{code}");
	}
}
