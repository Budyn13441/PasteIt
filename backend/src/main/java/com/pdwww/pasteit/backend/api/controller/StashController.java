package com.pdwww.pasteit.backend.api.controller;

import com.pdwww.pasteit.backend.api.dto.request.ProlongStashRequestDto;
import com.pdwww.pasteit.backend.api.dto.response.CreateStashResponseDto;
import com.pdwww.pasteit.backend.api.dto.response.StashViewResponseDto;
import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Validated
public class StashController {

	@PostMapping("/new")
	public ResponseEntity<CreateStashResponseDto> createNewStash() {
		throw new UnsupportedOperationException("Endpoint not implemented yet: POST /api/v1/new");
	}

	@GetMapping("/view/{code}")
	public ResponseEntity<StashViewResponseDto> viewStash(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.STASH_CODE_REGEX, message = "code contains unsupported characters") String code) {
		throw new UnsupportedOperationException("Endpoint not implemented yet: GET /api/v1/view/{code}");
	}

	@PostMapping("/prolong/{code}")
	public ResponseEntity<Void> prolongStash(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.STASH_CODE_REGEX, message = "code contains unsupported characters") String code,
			@Valid @RequestBody ProlongStashRequestDto request) {
		throw new UnsupportedOperationException("Endpoint not implemented yet: POST /api/v1/prolong/{code}");
	}

	@PostMapping("/make-readonly/{code}")
	public ResponseEntity<Void> makeStashReadOnly(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.STASH_CODE_REGEX, message = "code contains unsupported characters") String code) {
		throw new UnsupportedOperationException("Endpoint not implemented yet: POST /api/v1/make-readonly/{code}");
	}
}
