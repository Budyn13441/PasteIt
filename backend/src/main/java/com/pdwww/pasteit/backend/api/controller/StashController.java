package com.pdwww.pasteit.backend.api.controller;

import com.pdwww.pasteit.backend.api.dto.request.ProlongStashRequestDto;
import com.pdwww.pasteit.backend.api.dto.response.CreateStashResponseDto;
import com.pdwww.pasteit.backend.api.dto.response.StashViewResponseDto;
import com.pdwww.pasteit.backend.api.mapper.Mapper;
import com.pdwww.pasteit.backend.api.model.StashView;
import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pdwww.pasteit.backend.api.storage.StashStorage;

@RestController
@RequestMapping("/api/v1")
@Validated
public class StashController {
	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(StashController.class.getName());

	private final Mapper mapper;

	public StashController(Mapper mapper) {
		this.mapper = mapper;
	}

	@PostMapping("/new")
	public ResponseEntity<CreateStashResponseDto> createNewStash() {
		logger.info("Received request to create new stash.");
		StashStorage stash = StashStorage.createNew();
		CreateStashResponseDto responseDto = new CreateStashResponseDto(stash.getCode());
		return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
	}

	@GetMapping("/view/{code}")
	public ResponseEntity<StashViewResponseDto> viewStash(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.CODE_REGEX, message = "code contains unsupported characters") String code) {
		logger.info("Received request to view stash with code: " + code);

		StashStorage stash = StashStorage.getFor(code);
		StashView view = stash.getView();
		StashViewResponseDto responseDto = mapper.toStashViewResponseDto(view);

		return new ResponseEntity<>(responseDto, HttpStatus.OK);
	}

	@PostMapping("/prolong/{code}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> prolongStash(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.CODE_REGEX, message = "code contains unsupported characters") String code,
			@Valid @RequestBody ProlongStashRequestDto request) {
		StashStorage stash = StashStorage.getFor(code);
		stash.prolongExpiration(request.newExpirationDate());
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PostMapping("/make-readonly/{code}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> makeStashReadOnly(
			@PathVariable @NotBlank @Size(max = 128) @Pattern(regexp = ValidationPatterns.CODE_REGEX, message = "code contains unsupported characters") String code) {
		StashStorage stash = StashStorage.getFor(code);
		stash.makeReadOnly();
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}
