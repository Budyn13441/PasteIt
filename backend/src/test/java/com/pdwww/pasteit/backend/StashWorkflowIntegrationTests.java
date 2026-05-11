package com.pdwww.pasteit.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.OffsetDateTime;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"stashes.path=build/test-workflow/stashes",
		"stashes.meta.path=build/test-workflow/stashes-meta"
})
class StashWorkflowIntegrationTests {
	private static final Path STASHES_PATH = Path.of("build/test-workflow/stashes");
	private static final Path STASHES_META_PATH = Path.of("build/test-workflow/stashes-meta");
	private static final Logger logger = Logger.getLogger(StashWorkflowIntegrationTests.class.getName());

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private TestInfo testName;

	@BeforeEach
	void resetStorage(TestInfo info) throws IOException {
		testName = info;
		resetDirectory(STASHES_PATH);
		resetDirectory(STASHES_META_PATH);
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@AfterEach
	void printTestPassed() {
		logger.info("✓ Test PASSED: " + testName.getDisplayName());
		System.err.println("\n✓✓✓ Test PASSED: " + testName.getDisplayName() + " ✓✓✓\n");
	}

	@Test
	void fullWorkflowShouldFollowContractAndPersistState() throws Exception {
		String code = createStash();
		byte[] originalFileContent = "hello from workflow".getBytes();

		uploadFile(code, "/", "hello.txt", originalFileContent);

		JsonNode initialView = viewStash(code);
		JsonNode initialFileNode = findNodeByPath(initialView.path("file_tree"), "/hello.txt");
		assertThat(initialFileNode).isNotNull();
		assertThat(initialFileNode.path("file").path("category").asText()).isEqualTo("TEXT");

		OffsetDateTime newExpirationDate = OffsetDateTime.now().plusDays(2).withNano(0);
		mockMvc.perform(post("/api/v1/prolong/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"new_expiration_date\":\"" + newExpirationDate + "\"}"))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/new-folder/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"parent_path\":\"/\",\"name\":\"docs\"}"))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/rename/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"old_path\":\"/hello.txt\",\"new_path\":\"/docs/renamed.txt\"}"))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/update-category/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"file_path\":\"/docs/renamed.txt\",\"category\":\"OTHER\"}"))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/download/{code}", code).queryParam("path", "/docs/renamed.txt"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
				.andExpect(content().bytes(originalFileContent));

		mockMvc.perform(delete("/api/v1/delete/{code}", code).queryParam("path", "/docs/renamed.txt"))
				.andExpect(status().isNoContent());

		JsonNode finalView = viewStash(code);
		assertThat(OffsetDateTime.parse(finalView.path("valid_until").asText())).isAfter(OffsetDateTime.now().plusDays(1));

		JsonNode docsNode = findNodeByPath(finalView.path("file_tree"), "/docs");
		assertThat(docsNode).isNotNull();
		assertThat(docsNode.path("is_directory").asBoolean()).isTrue();
		assertThat(findNodeByPath(finalView.path("file_tree"), "/docs/renamed.txt")).isNull();
	}

	@Test
	void makeReadOnlyShouldBlockFurtherMutations() throws Exception {
		String code = createStash();
		uploadFile(code, "/", "locked.txt", "lock me".getBytes());

		mockMvc.perform(post("/api/v1/make-readonly/{code}", code))
				.andExpect(status().isNoContent());

		MockMultipartFile secondFile = new MockMultipartFile(
				"data",
				"new.txt",
				MediaType.TEXT_PLAIN_VALUE,
				"forbidden".getBytes());

		mockMvc.perform(multipart("/api/v1/upload/{code}", code)
						.file(secondFile)
						.param("is_directory", "false")
						.param("parent_path", "/")
						.param("name", "new.txt"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	void viewNonExistentStashShouldReturn404() throws Exception {
		mockMvc.perform(get("/api/v1/view/{code}", "nonexistent"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}

	@Test
	void uploadToNonExistentStashShouldReturn404() throws Exception {
		MockMultipartFile file = new MockMultipartFile("data", "test.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());
		mockMvc.perform(multipart("/api/v1/upload/{code}", "nonexistent")
						.file(file)
						.param("is_directory", "false")
						.param("parent_path", "/")
						.param("name", "test.txt"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteNonExistentFileShouldReturn404() throws Exception {
		String code = createStash();
		mockMvc.perform(delete("/api/v1/delete/{code}", code).queryParam("path", "/nonexistent.txt"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}

	@Test
	void renameToExistingNameShouldReturn409Conflict() throws Exception {
		String code = createStash();
		uploadFile(code, "/", "file1.txt", "content1".getBytes());
		uploadFile(code, "/", "file2.txt", "content2".getBytes());

		mockMvc.perform(post("/api/v1/rename/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"old_path\":\"/file1.txt\",\"new_path\":\"/file2.txt\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("CONFLICT"));
	}

	@Test
	void createFolderWithDuplicateNameShouldReturn409() throws Exception {
		String code = createStash();
		mockMvc.perform(post("/api/v1/new-folder/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"parent_path\":\"/\",\"name\":\"docs\"}"))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/new-folder/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"parent_path\":\"/\",\"name\":\"docs\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void uploadFileWithoutParentPathShouldFail() throws Exception {
		String code = createStash();
		// Try uploading to a parent path with invalid format
		MockMultipartFile file = new MockMultipartFile("data", "test.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());
		// 507 Insufficient Storage is returned when path creation fails
		mockMvc.perform(multipart("/api/v1/upload/{code}", code)
						.file(file)
						.param("is_directory", "false")
						.param("parent_path", "/nonexistent")
						.param("name", "test.txt"))
				.andExpect(status().isInsufficientStorage());
	}

	@Test
	void pathTraversalWithDotDotShouldReturn400() throws Exception {
		String code = createStash();
		// Paths with .. segments are invalid
		mockMvc.perform(delete("/api/v1/delete/{code}", code).queryParam("path", "/folder/../etc/passwd"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void pathWithSpecialCharactersShouldReturn400() throws Exception {
		String code = createStash();
		// Paths with unallowed characters should be rejected
		mockMvc.perform(delete("/api/v1/delete/{code}", code).queryParam("path", "/file@invalid"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void invalidCodeFormatShouldReturn400() throws Exception {
		mockMvc.perform(get("/api/v1/view/{code}", "invalid@code!"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void uploadFileToReadOnlyShouldReturn403() throws Exception {
		String code = createStash();
		uploadFile(code, "/", "file.txt", "content".getBytes());
		mockMvc.perform(post("/api/v1/make-readonly/{code}", code))
				.andExpect(status().isNoContent());

		MockMultipartFile newFile = new MockMultipartFile("data", "new.txt", MediaType.TEXT_PLAIN_VALUE, "new".getBytes());
		mockMvc.perform(multipart("/api/v1/upload/{code}", code)
						.file(newFile)
						.param("is_directory", "false")
						.param("parent_path", "/")
						.param("name", "new.txt"))
				.andExpect(status().isForbidden());
	}

	@Test
	void deleteFileInReadOnlyShouldReturn403() throws Exception {
		String code = createStash();
		uploadFile(code, "/", "file.txt", "content".getBytes());
		mockMvc.perform(post("/api/v1/make-readonly/{code}", code))
				.andExpect(status().isNoContent());

		mockMvc.perform(delete("/api/v1/delete/{code}", code).queryParam("path", "/file.txt"))
				.andExpect(status().isForbidden());
	}

	@Test
	void updateCategoryOnReadOnlyShouldReturn403() throws Exception {
		String code = createStash();
		uploadFile(code, "/", "file.txt", "content".getBytes());
		mockMvc.perform(post("/api/v1/make-readonly/{code}", code))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/update-category/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"file_path\":\"/file.txt\",\"category\":\"IMAGE\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void updateCategoryOnNonExistentFileShouldReturn404() throws Exception {
		String code = createStash();
		mockMvc.perform(post("/api/v1/update-category/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"file_path\":\"/nonexistent.txt\",\"category\":\"TEXT\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void nestedFolderStructureShouldMaintainHierarchy() throws Exception {
		String code = createStash();
		uploadFile(code, "/", "root.txt", "root content".getBytes());

		mockMvc.perform(post("/api/v1/new-folder/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"parent_path\":\"/\",\"name\":\"level1\"}"))
				.andExpect(status().isNoContent());

		uploadFile(code, "/level1", "level1.txt", "level1 content".getBytes());

		mockMvc.perform(post("/api/v1/new-folder/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"parent_path\":\"/level1\",\"name\":\"level2\"}"))
				.andExpect(status().isNoContent());

		uploadFile(code, "/level1/level2", "deep.txt", "deep content".getBytes());

		JsonNode view = viewStash(code);
		JsonNode level1 = findNodeByPath(view.path("file_tree"), "/level1");
		assertThat(level1).isNotNull();
		assertThat(level1.path("is_directory").asBoolean()).isTrue();

		JsonNode level2 = findNodeByPath(view.path("file_tree"), "/level1/level2");
		assertThat(level2).isNotNull();
		assertThat(level2.path("is_directory").asBoolean()).isTrue();

		JsonNode deepFile = findNodeByPath(view.path("file_tree"), "/level1/level2/deep.txt");
		assertThat(deepFile).isNotNull();
	}

	@Test
	void downloadNonExistentPathShouldReturn404() throws Exception {
		String code = createStash();
		mockMvc.perform(get("/api/v1/download/{code}", code).queryParam("path", "/nonexistent.txt"))
				.andExpect(status().isNotFound());
	}

	@Test
	void downloadDirectoryWithoutZipFormatShouldReturn400() throws Exception {
		String code = createStash();
		mockMvc.perform(post("/api/v1/new-folder/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"parent_path\":\"/\",\"name\":\"folder\"}"))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/download/{code}", code).queryParam("path", "/folder"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void prolongStashShouldExtendExpiration() throws Exception {
		String code = createStash();
		JsonNode beforeProlong = viewStash(code);
		OffsetDateTime originalExpiration = OffsetDateTime.parse(beforeProlong.path("valid_until").asText());
		
		OffsetDateTime newExpirationDate = OffsetDateTime.now().plusDays(7);
		mockMvc.perform(post("/api/v1/prolong/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"new_expiration_date\":\"" + newExpirationDate + "\"}"))
				.andExpect(status().isNoContent());

		JsonNode afterProlong = viewStash(code);
		OffsetDateTime updatedExpiration = OffsetDateTime.parse(afterProlong.path("valid_until").asText());
		assertThat(updatedExpiration).isAfter(originalExpiration);
	}

	@Test
	void prolongReadOnlyStashShouldReturn403() throws Exception {
		String code = createStash();
		mockMvc.perform(post("/api/v1/make-readonly/{code}", code))
				.andExpect(status().isNoContent());

		OffsetDateTime newDate = OffsetDateTime.now().plusDays(7);
		mockMvc.perform(post("/api/v1/prolong/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"new_expiration_date\":\"" + newDate + "\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	void renameFileInReadOnlyShouldReturn403() throws Exception {
		String code = createStash();
		uploadFile(code, "/", "original.txt", "content".getBytes());
		mockMvc.perform(post("/api/v1/make-readonly/{code}", code))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/rename/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"old_path\":\"/original.txt\",\"new_path\":\"/renamed.txt\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void updateCategoryOnReadOnlyFileShouldReturn403() throws Exception {
		String code = createStash();
		uploadFile(code, "/", "image.png", "not really an image".getBytes());
		mockMvc.perform(post("/api/v1/make-readonly/{code}", code))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/update-category/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"file_path\":\"/image.png\",\"category\":\"TEXT\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	void createFolderInReadOnlyShouldReturn403() throws Exception {
		String code = createStash();
		mockMvc.perform(post("/api/v1/make-readonly/{code}", code))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/new-folder/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"parent_path\":\"/\",\"name\":\"newdir\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void downloadExistingFileShouldReturnContent() throws Exception {
		String code = createStash();
		byte[] fileContent = "test file content".getBytes();
		uploadFile(code, "/", "download.txt", fileContent);

		MvcResult result = mockMvc.perform(get("/api/v1/download/{code}", code).queryParam("path", "/download.txt"))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(fileContent);
	}

	@Test
	void deleteFileAndVerifyItRemoved() throws Exception {
		String code = createStash();
		uploadFile(code, "/", "file-to-delete.txt", "content".getBytes());

		JsonNode beforeDelete = viewStash(code);
		JsonNode file = findNodeByPath(beforeDelete.path("file_tree"), "/file-to-delete.txt");
		assertThat(file).isNotNull();

		mockMvc.perform(delete("/api/v1/delete/{code}", code).queryParam("path", "/file-to-delete.txt"))
				.andExpect(status().isNoContent());

		JsonNode afterDelete = viewStash(code);
		JsonNode deletedFile = findNodeByPath(afterDelete.path("file_tree"), "/file-to-delete.txt");
		assertThat(deletedFile).isNull();
	}

	@Test
	void renameFileSuccessfully() throws Exception {
		String code = createStash();
		uploadFile(code, "/", "oldname.txt", "content".getBytes());

		mockMvc.perform(post("/api/v1/rename/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"old_path\":\"/oldname.txt\",\"new_path\":\"/newname.txt\"}"))
				.andExpect(status().isNoContent());

		JsonNode view = viewStash(code);
		JsonNode renamedFile = findNodeByPath(view.path("file_tree"), "/newname.txt");
		assertThat(renamedFile).isNotNull();
		JsonNode oldFile = findNodeByPath(view.path("file_tree"), "/oldname.txt");
		assertThat(oldFile).isNull();
	}

	@Test
	void updateFileCategoryShouldWork() throws Exception {
		String code = createStash();
		uploadFile(code, "/", "data.bin", "binary content".getBytes());

		mockMvc.perform(post("/api/v1/update-category/{code}", code)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"file_path\":\"/data.bin\",\"category\":\"IMAGE\"}"))
				.andExpect(status().isNoContent());

		JsonNode view = viewStash(code);
		JsonNode file = findNodeByPath(view.path("file_tree"), "/data.bin");
		assertThat(file).isNotNull();
		assertThat(file.path("file").path("category").asText()).isEqualTo("IMAGE");
	}

	private String createStash() throws Exception {
		MvcResult createResult = mockMvc.perform(post("/api/v1/new"))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andReturn();

		JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsByteArray());
		String code = createBody.path("code").asText();
		assertThat(code).matches("^[A-Za-z0-9_]{1,128}$");
		return code;
	}

	private void uploadFile(String code, String parentPath, String name, byte[] contentBytes) throws Exception {
		MockMultipartFile multipartFile = new MockMultipartFile("data", name, MediaType.TEXT_PLAIN_VALUE, contentBytes);
		mockMvc.perform(multipart("/api/v1/upload/{code}", code)
						.file(multipartFile)
						.param("is_directory", "false")
						.param("parent_path", parentPath)
						.param("name", name))
				.andExpect(status().isNoContent());
	}

	private JsonNode viewStash(String code) throws Exception {
		MvcResult viewResult = mockMvc.perform(get("/api/v1/view/{code}", code))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andReturn();
		return objectMapper.readTree(viewResult.getResponse().getContentAsByteArray());
	}

	private JsonNode findNodeByPath(JsonNode node, String expectedPath) {
		if (node == null || node.isNull()) {
			return null;
		}
		if (expectedPath.equals(node.path("path").asText())) {
			return node;
		}
		JsonNode children = node.path("children");
		if (!children.isArray()) {
			return null;
		}
		for (JsonNode child : children) {
			JsonNode found = findNodeByPath(child, expectedPath);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static void resetDirectory(Path directory) throws IOException {
		if (Files.exists(directory)) {
			Files.walkFileTree(directory, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		}
		Files.createDirectories(directory);
	}

}
