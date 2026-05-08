package com.pdwww.pasteit.backend.api.controller;

import com.pdwww.pasteit.backend.api.dto.request.CreateFolderRequestDto;
import com.pdwww.pasteit.backend.api.dto.request.DeleteEntryQueryDto;
import com.pdwww.pasteit.backend.api.dto.request.DownloadEntryQueryDto;
import com.pdwww.pasteit.backend.api.dto.request.RenameEntryRequestDto;
import com.pdwww.pasteit.backend.api.dto.request.UpdateFileCategoryRequestDto;
import com.pdwww.pasteit.backend.api.dto.request.UploadEntryRequestDto;
import com.pdwww.pasteit.backend.api.exception.ServerResourceException;
import com.pdwww.pasteit.backend.api.storage.StashStorage;
import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.zip.ZipInputStream;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1")
@Validated
public class StashContentController {
	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(StashContentController.class.getName());

	@PostMapping(value = "/upload/{code}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> uploadEntry(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.CODE_REGEX, message = "code contains unsupported characters") String code,
			@Valid @ModelAttribute UploadEntryRequestDto request) {
		logger.info("Received request to upload entry to stash with code: " + code);
		StashStorage stash = StashStorage.getFor(code);

		try {
			if (request.is_directory()) {
				stash.uploadDirectory(Paths.get(request.parent_path()), request.name(),
						new ZipInputStream(request.data().getInputStream()));
			} else {
				stash.uploadFile(Paths.get(request.parent_path()), request.name(), request.data().getInputStream());
			}
		} catch (IOException e) {
			logger.severe("Failed to upload entry: " + e.getMessage());
			throw new ServerResourceException("Failed to upload entry", e);
		}

		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@GetMapping(value = "/download/{code}", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<StreamingResponseBody> downloadEntry(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.CODE_REGEX, message = "code contains unsupported characters") String code,
			@Valid @ModelAttribute DownloadEntryQueryDto query) {
		logger.info("Received request to download entry from stash with code: " + code);
		StashStorage stash = StashStorage.getFor(code);

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		stash.download(Paths.get(query.path()), query.format(), buffer);

		// ???
		StreamingResponseBody body = outputStream -> {
			outputStream.write(buffer.toByteArray());
		};

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(body);
	}

	@DeleteMapping("/delete/{code}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> deleteEntry(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.CODE_REGEX, message = "code contains unsupported characters") String code,
			@Valid @ModelAttribute DeleteEntryQueryDto query) {
		throw new UnsupportedOperationException("Endpoint not implemented yet: DELETE /api/v1/delete/{code}");
	}

	@PostMapping("/rename/{code}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> renameEntry(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.CODE_REGEX, message = "code contains unsupported characters") String code,
			@Valid @RequestBody RenameEntryRequestDto request) {
		throw new UnsupportedOperationException("Endpoint not implemented yet: POST /api/v1/rename/{code}");
	}

	@PostMapping("/new-folder/{code}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> createFolder(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.CODE_REGEX, message = "code contains unsupported characters") String code,
			@Valid @RequestBody CreateFolderRequestDto request) {
		throw new UnsupportedOperationException("Endpoint not implemented yet: POST /api/v1/new-folder/{code}");
	}

	@PostMapping("/update-category/{code}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> updateCategory(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.CODE_REGEX, message = "code contains unsupported characters") String code,
			@Valid @RequestBody UpdateFileCategoryRequestDto request) {
		throw new UnsupportedOperationException("Endpoint not implemented yet: POST /api/v1/update-category/{code}");
	}
}
