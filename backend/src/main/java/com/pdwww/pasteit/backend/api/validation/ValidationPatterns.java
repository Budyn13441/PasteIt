package com.pdwww.pasteit.backend.api.validation;

import java.nio.file.Path;

import com.pdwww.pasteit.backend.api.exception.InvalidRequestException;

public final class ValidationPatterns {

	/**
	 * Letters, digits, underscore; length 1–128.
	 * Examples: "abb18adB_", "_test123"
	 */
	public static final String CODE_REGEX = "^[A-Za-z0-9_]{1,128}$";

	public static void verifyCode(String code) {
		if (code == null || !code.matches(CODE_REGEX)) {
			throw new InvalidRequestException("Invalid code format");
		}
	}

	/**
	 * Valid absolute paths:
	 * /a, /a/b, /file.txt, /, /a/b/file.txt/
	 *
	 * Rules:
	 * - Must start with '/'
	 * - No '.' or '..' segments
	 * - Allowed: letters, digits, '_', '-', '.', space
	 */
	public static final String ABSOLUTE_PATH_REGEX = "^/(?!.*(?:^|/)\\.\\.?(/|$))([A-Za-z0-9._\\- ]+/)*[A-Za-z0-9._\\- ]*/?$";

	public static void verifyAbsolutePath(String path) {
		if (path == null || !path.matches(ABSOLUTE_PATH_REGEX)) {
			throw new InvalidRequestException("Invalid path format");
		}
	}

	/**
	 * ZIP entry names:
	 * file.txt, a/b/c.txt, images/logo.png
	 *
	 * Rules:
	 * - No leading '/'
	 * - No '.' or '..' segments
	 * - No control chars or backslashes
	 * - Length 1–255
	 */
	public static final String ZIP_ENTRY_REGEX = "^(?!/)(?!.*(?:^|/)\\.\\.?(/|$))[^\\p{Cntrl}\\\\]{1,255}$";

	public static void verifyZipEntry(String entry) {
		if (entry == null || !entry.matches(ZIP_ENTRY_REGEX)) {
			throw new InvalidRequestException("Invalid ZIP entry format");
		}
	}

	/**
	 * File names (no path allowed):
	 * file.txt, archive.tar.gz, image
	 *
	 * Rules:
	 * - No '/', '\', or control chars
	 * - Must not be '.' or '..'
	 * - Allows multiple extensions
	 * - Length 1–255
	 */
	public static final String FILE_NAME_REGEX = "^(?!\\.\\.?$)[^/\\\\\\p{Cntrl}]{1,255}$";

	public static void verifyFileName(String name) {
		if (name == null || !name.matches(FILE_NAME_REGEX)) {
			throw new InvalidRequestException("Invalid file name format");
		}
	}

	/**
	 * ZIP format validation (empty or 'zip')
	 */
	public static final String FORMAT_REGEX = "^zip$";

	public static void verifyFormat(String value) {
		if (value == null || !value.matches(FORMAT_REGEX)) {
			throw new InvalidRequestException("Invalid format");
		}
	}

	/**
	 * Verify a user-facing viewPath inside a stash directory.
	 *
	 * Example:
	 * stashPath = ./stashes/123
	 * viewPath = /a/b/c.txt
	 * result = ./stashes/123/a/b/c.txt
	 *
	 * Security:
	 * - prevents path traversal (Zip Slip style attacks)
	 * - ensures result stays inside stashPath
	 */
	public static Path verifyInsideStash(Path stashPath, String viewPath) {
		if (stashPath == null || viewPath == null) {
			throw new IllegalArgumentException("Null input");
		}

		// normalize view path (strip leading slash so it becomes relative)
		String normalizedView = viewPath.startsWith("/")
				? viewPath.substring(1)
				: viewPath;

		Path resolved = stashPath.resolve(normalizedView).normalize();

		if (!resolved.startsWith(stashPath.normalize())) {
			throw new InvalidRequestException("Path escapes stash directory");
		}

		return resolved;
	}
}