package com.pdwww.pasteit.backend.api.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.pdwww.pasteit.backend.api.exception.InvalidRequestException;
import com.pdwww.pasteit.backend.api.exception.ResourceAlreadyExistsException;
import com.pdwww.pasteit.backend.api.exception.ResourceNotFoundException;
import com.pdwww.pasteit.backend.api.exception.ServerResourceException;
import com.pdwww.pasteit.backend.api.exception.StashReadOnlyException;
import com.pdwww.pasteit.backend.api.model.NodeCategory;
import com.pdwww.pasteit.backend.api.model.StashMeta;
import com.pdwww.pasteit.backend.api.model.StashNode;
import com.pdwww.pasteit.backend.api.model.StashView;
import com.pdwww.pasteit.backend.api.util.CodeGenerator;
import com.pdwww.pasteit.backend.api.util.FileUtils;
import com.pdwww.pasteit.backend.api.validation.ValidationPatterns;

import tools.jackson.databind.ObjectMapper;

@Component
public class StashStorage {
    private static final Logger logger = Logger.getLogger(StashStorage.class.getName());

    private static Path ROOT_DIR;
    private static Path META_DIR;

    private static final int EXPIRATION_MINUTES_DEFAULT = 60 * 24; // 24 hours

    private String code;
    private Path stashPath;
    private Path metaPath;
    private StashMeta meta;

    private boolean expired = false;

    public StashStorage() {
    }

    @Value("${stashes.path}")
    public void setStashesPath(String stashesPath) {
        ROOT_DIR = Paths.get(stashesPath);
        try {
            Files.createDirectories(ROOT_DIR);
        } catch (IOException e) {
            logger.warning("Failed to create stashes directory: " + e.getMessage());
        }
    }

    @Value("${stashes.meta.path}")
    public void setStashesMetaPath(String stashesMetaPath) {
        META_DIR = Paths.get(stashesMetaPath);
        try {
            Files.createDirectories(META_DIR);
        } catch (IOException e) {
            logger.warning("Failed to create stashes-meta directory: " + e.getMessage());
        }
    }

    private StashStorage(String code, Path stashPath, Path metaPath) {
        this.code = code;
        this.stashPath = stashPath;
        this.metaPath = metaPath;
        refreshMeta();
    }

    public String getCode() {
        return code;
    }

    public StashMeta getMeta() {
        return meta;
    }

    public static StashStorage getFor(String code) {
        logger.info("Attempting to retrieve stash with code '" + code + "'");
        ValidationPatterns.verifyCode(code);

        Path stashPath = ROOT_DIR.resolve(code).normalize();
        Path metaPath = META_DIR.resolve(code + ".json").normalize();

        if (!Files.isDirectory(stashPath)) {
            throw new ResourceNotFoundException(code);
        }

        if (!Files.exists(metaPath)) {
            throw new ResourceNotFoundException(code);
        }

        logger.info("Valid stash with code '" + code + "' found at " + stashPath.toString());
        StashStorage res = new StashStorage(code, stashPath, metaPath);

        logger.info("Expires on " + res.getMeta().expirationDate() + ", read-only: " + res.getMeta().readOnly());

        res.refreshAndVerifyNotExpired();
        return res;
    }

    public void refreshAndVerifyNotExpired() {
        refreshMeta();
        if (expired) {
            throw new ResourceNotFoundException(code);
        }
        if (meta.expirationDate().isBefore(java.time.ZonedDateTime.now())) {
            logger.info("Stash with code '" + code + "' has expired and will be deleted");
            delete();
            logger.info("Stash with code '" + code + "' deleted successfully");
            expired = true;
            throw new ResourceNotFoundException(code);
        }
    }

    public void verifyNotReadOnly() {
        if (meta.readOnly()) {
            throw new StashReadOnlyException();
        }
    }

    public static StashStorage createNew() {
        logger.info("Attempting to create new stash.");

        String code = CodeGenerator.generateUniqueCode();
        ValidationPatterns.verifyCode(code);

        Path stashPath = ROOT_DIR.resolve(code);
        Path metaPath = META_DIR.resolve(code + ".json");

        try {
            Files.createDirectory(stashPath);
            logger.info("Created new stash directory at " + stashPath.toString());

            Map<String, NodeCategory> nodeCategories = new java.util.HashMap<>();
            nodeCategories.put("/", NodeCategory.DIRECTORY);

            StashMeta meta = new StashMeta(
                    java.time.ZonedDateTime.now().plusMinutes(EXPIRATION_MINUTES_DEFAULT),
                    false, nodeCategories);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(Files.newOutputStream(metaPath), meta);

            logger.info("Created metadata for new stash at " + metaPath.toString());
        } catch (Exception e) {
            logger.severe("Failed to create stash directory: " + e.getMessage());
            try {
                FileUtils.deleteDirectory(stashPath);
                FileUtils.deleteDirectory(metaPath);
            } catch (Exception ex) {
                logger.warning("Failed to clean up stash directory: " + ex.getMessage());
            }
            throw new ServerResourceException("Failed to create stash directory", e);
        }

        return new StashStorage(code, stashPath, metaPath);
    }

    public static boolean exists(String code) {
        Path stashPath = ROOT_DIR.resolve(code).normalize();
        return Files.isDirectory(stashPath);
    }

    private void refreshMeta() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.meta = mapper.readValue(Files.newInputStream(metaPath), StashMeta.class);
        } catch (Exception e) {
            logger.severe("Failed to load stash metadata: " + e.getMessage());
            throw new ServerResourceException("Failed to load stash metadata", e);
        }
    }

    private NodeCategory getNodeCategory(Path path) {
        try {
            return meta.nodeCategories().get(path.toString());
        } catch (Exception e) {
            logger.severe("Failed to get node category for path '" + path.toString() + "': " + e.getMessage());
            throw new ServerResourceException("Failed to get node category for path '" + path.toString() + "'", e);
        }
    }

    private void setNodeCategory(Path path, NodeCategory category) {
        meta.nodeCategories().put(path.toString(), category);
    }

    private void saveMeta() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(Files.newOutputStream(metaPath), meta);
        } catch (Exception e) {
            logger.severe("Failed to sync stash metadata: " + e.getMessage());
            throw new ServerResourceException("Failed to sync stash metadata", e);
        }
    }

    public void delete() {
        try {
            FileUtils.deleteDirectory(stashPath);
            FileUtils.deleteDirectory(metaPath);
        } catch (Exception e) {
            logger.severe("Failed to delete stash: " + e.getMessage());
            throw new ServerResourceException("Failed to delete stash", e);
        }
    }

    private Path relativeViewPath(Path absolutePath) {
        return StashView.ROOT_PATH.resolve(stashPath.relativize(absolutePath)).normalize();
    }

    public StashView getView() {
        logger.info("Generating view for stash with code '" + code + "'");
        refreshAndVerifyNotExpired();

        Deque<StashNode> stack = new ArrayDeque<>();
        stack.push(new StashNode(StashView.ROOT_PATH, "/", 0, NodeCategory.DIRECTORY, new ArrayList<>()));

        try {
            Files.walkFileTree(stashPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.equals(stashPath)) {
                        return FileVisitResult.CONTINUE;
                    }
                    Path relativePath = relativeViewPath(dir);
                    logger.fine(
                            "Visiting directory: " + dir.toString() + " | stashPath: " + stashPath.toString()
                                    + " | relativePath: " + relativePath.toString());

                    StashNode node = new StashNode(relativePath,
                            dir.getFileName().toString(),
                            0,
                            NodeCategory.DIRECTORY, new ArrayList<>());
                    stack.peek().addChild(node);
                    stack.push(node);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativePath = relativeViewPath(file);
                    StashNode node = new StashNode(relativePath,
                            file.getFileName().toString(),
                            attrs.size(),
                            getNodeCategory(relativePath), null);
                    stack.peek().addChild(node);
                    logger.fine("Visiting file: " + file.toString() + " | stashPath: " + stashPath.toString()
                            + " | relativePath: " + relativePath.toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Path relativePath = relativeViewPath(dir);
                    if (dir.equals(stashPath)) {
                        return FileVisitResult.CONTINUE;
                    }
                    logger.fine(
                            "Finished visiting directory: " + dir.toString() + " | stashPath: " + stashPath.toString()
                                    + " | relativePath: " + relativePath.toString());
                    stack.pop();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            logger.severe("Failed to read stash contents: " + e.getMessage());
            throw new ServerResourceException("Failed to read stash contents", e);
        }

        refreshMeta();
        return new StashView(code, meta, stack.peek());
    }

    public void prolongExpiration(ZonedDateTime newExpirationDate) {
        refreshAndVerifyNotExpired();
        verifyNotReadOnly();
        meta = new StashMeta(newExpirationDate, meta.readOnly(), meta.nodeCategories());
        saveMeta();
    }

    public void makeReadOnly() {
        refreshAndVerifyNotExpired();
        verifyNotReadOnly();
        meta = new StashMeta(meta.expirationDate(), true, meta.nodeCategories());
        saveMeta();
    }

    private NodeCategory guessCategoryFromName(String name) {
        if (name.endsWith("/")) {
            return NodeCategory.DIRECTORY;
        } else if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif")) {
            return NodeCategory.IMAGE;
        } else if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".java") || name.endsWith(".py")
                || name.endsWith(".c") || name.endsWith(".cpp") || name.endsWith(".js") || name.endsWith(".html")
                || name.endsWith(".css")) {
            return NodeCategory.TEXT;
        } else {
            return NodeCategory.OTHER;
        }
    }

    public void uploadFile(Path parentPath, String name, InputStream content) {
        logger.info("Uploading file to stash with code '" + code + "' at path '" + parentPath.toString()
                + "' with name '" + name + "'");
        refreshAndVerifyNotExpired();
        verifyNotReadOnly();

        ValidationPatterns.verifyAbsolutePath(parentPath.toString());
        Path targetPath;
        if (name.isEmpty()) {
            String fileName = "file.txt";
            for (int i = 1; Files.exists(
                    ValidationPatterns.verifyInsideStash(stashPath, parentPath.resolve(fileName).toString())); i++) {
                fileName = "file(" + i + ").txt";
            }
            name = fileName;
            targetPath = ValidationPatterns.verifyInsideStash(stashPath, parentPath.resolve(fileName).toString());
        } else {
            targetPath = ValidationPatterns.verifyInsideStash(stashPath, parentPath.resolve(name).toString());
        }
        logger.fine("Resolved target path for upload: " + targetPath.toString());

        if (Files.exists(targetPath)) {
            throw new ResourceAlreadyExistsException(relativeViewPath(targetPath).toString());
        }

        try {
            Files.copy(content, targetPath);
            setNodeCategory(relativeViewPath(targetPath), guessCategoryFromName(name));
            saveMeta();
        } catch (Exception e) {
            logger.severe("Failed to upload file: " + e.getMessage());
            throw new ServerResourceException("Failed to upload file", e);
        }
    }

    public void uploadDirectory(Path parentPath, String name, ZipInputStream content) {
        logger.info("Uploading directory to stash with code '" + code + "' at path '" + parentPath.toString()
                + "' with name '" + name + "'");
        refreshAndVerifyNotExpired();
        verifyNotReadOnly();

        ValidationPatterns.verifyAbsolutePath(parentPath.toString());
        Path dirPath;
        if (name.isEmpty()) {
            dirPath = ValidationPatterns.verifyInsideStash(stashPath, parentPath.toString());
        } else {
            dirPath = ValidationPatterns.verifyInsideStash(stashPath, parentPath.resolve(name).toString());
            if (Files.exists(dirPath)) {
                throw new ResourceAlreadyExistsException(relativeViewPath(dirPath).toString());
            }
            try {
                Files.createDirectories(dirPath);
                setNodeCategory(relativeViewPath(dirPath), NodeCategory.DIRECTORY);
            } catch (Exception e) {
                logger.severe("Failed to create directory for upload: " + e.getMessage());
                throw new ServerResourceException("Failed to create directory for upload", e);
            }
        }
        logger.fine("Resolved target directory path for upload: " + dirPath.toString());

        if (!Files.exists(dirPath)) {
            throw new ResourceNotFoundException(relativeViewPath(dirPath).toString());
        }
        if (!Files.isDirectory(dirPath)) {
            throw new InvalidRequestException(
                    "Path '" + relativeViewPath(dirPath).toString() + "' is not a directory");
        }

        ZipEntry entry;
        boolean hasEntries = false;
        try {
            while ((entry = content.getNextEntry()) != null) {
                hasEntries = true;
                ValidationPatterns.verifyZipEntry(entry.getName());
                Path resultPath = dirPath.resolve(entry.getName()).normalize();
                logger.fine("Processing zip entry: " + entry.getName() + " | resultPath: " + resultPath.toString());
                if (entry.isDirectory()) {
                    try {
                        Files.createDirectories(resultPath);
                        setNodeCategory(relativeViewPath(resultPath), NodeCategory.DIRECTORY);
                    } catch (Exception e) {
                        logger.severe("Failed to create directory during upload: " + e.getMessage());
                        throw new ServerResourceException("Failed to create directory during upload", e);
                    }
                } else {
                    try {
                        Files.copy(content, resultPath);
                        setNodeCategory(relativeViewPath(resultPath), guessCategoryFromName(entry.getName()));
                    } catch (Exception e) {
                        logger.severe("Failed to upload file during directory upload: " + e.getMessage());
                        throw new ServerResourceException("Failed to upload file during directory upload", e);
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to read zip content: " + e.getMessage());
            throw new ServerResourceException("Failed to read zip content", e);
        }

        if (hasEntries) {
            saveMeta();
        } else {
            logger.severe("Zip file is empty, no entries found to upload");
            throw new InvalidRequestException("Zip file is empty, no entries found to upload");
        }
    }

    private void downloadFile(Path filePath, OutputStream output) {
        logger.info("Downloading file at path '" + filePath.toString() + "' from stash with code '" + code + "'");
        refreshAndVerifyNotExpired();

        ValidationPatterns.verifyAbsolutePath(filePath.toString());
        Path targetPath = ValidationPatterns.verifyInsideStash(stashPath, filePath.toString());

        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new ResourceNotFoundException(relativeViewPath(targetPath).toString());
        }

        try {
            Files.copy(targetPath, output);
        } catch (Exception e) {
            logger.severe("Failed to download file: " + e.getMessage());
            throw new ServerResourceException("Failed to download file", e);
        }
    }

    private void downloadFileAsZip(Path filePath, ZipOutputStream stream) {
        logger.info(
                "Downloading file at path '" + filePath.toString() + "' as zip from stash with code '" + code + "'");
        refreshAndVerifyNotExpired();

        ValidationPatterns.verifyAbsolutePath(filePath.toString());
        Path targetPath = ValidationPatterns.verifyInsideStash(stashPath, filePath.toString());

        if (!Files.exists(targetPath)) {
            throw new ResourceNotFoundException(relativeViewPath(targetPath).toString());
        }
        if (Files.isDirectory(targetPath)) {
            throw new InvalidRequestException(
                    "Path '" + relativeViewPath(targetPath).toString() + "' is a directory, not a file");
        }

        ZipEntry entry = new ZipEntry(targetPath.getFileName().toString());
        try {
            stream.putNextEntry(entry);
            Files.copy(targetPath, stream);
            stream.closeEntry();
        } catch (Exception e) {
            logger.severe("Failed to download file as zip: " + e.getMessage());
            throw new ServerResourceException("Failed to download file as zip", e);
        }
    }

    private void downloadDirectory(Path dirPath, ZipOutputStream stream) {
        logger.info("Downloading directory at path '" + dirPath.toString() + "' as zip from stash with code '" + code
                + "'");
        refreshAndVerifyNotExpired();

        ValidationPatterns.verifyAbsolutePath(dirPath.toString());
        Path targetPath = ValidationPatterns.verifyInsideStash(stashPath, dirPath.toString());

        if (!Files.exists(targetPath) || !Files.isDirectory(targetPath)) {
            throw new ResourceNotFoundException(relativeViewPath(targetPath).toString());
        }

        try {
            Files.walkFileTree(targetPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativePath = targetPath.relativize(file).normalize();
                    logger.fine(
                            "Adding file to zip: " + file.toString() + " | relativePath: " + relativePath.toString());
                    ZipEntry entry = new ZipEntry(relativePath.toString());
                    stream.putNextEntry(entry);
                    Files.copy(file, stream);
                    stream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            logger.severe("Failed to download directory as zip: " + e.getMessage());
            throw new ServerResourceException("Failed to download directory as zip", e);
        }
    }

    public void download(Path path, String format, OutputStream output) {
        refreshAndVerifyNotExpired();

        ValidationPatterns.verifyAbsolutePath(path.toString());
        Path targetPath = ValidationPatterns.verifyInsideStash(stashPath, path.toString());

        if (!Files.exists(targetPath)) {
            throw new ResourceNotFoundException(relativeViewPath(targetPath).toString());
        }

        if (format == null) {
            format = "raw";
        }

        if (format.equals("zip")) {
            if (Files.isDirectory(targetPath)) {
                downloadDirectory(path, new ZipOutputStream(output));
            } else {
                downloadFileAsZip(path, new ZipOutputStream(output));
            }
        } else {
            if (Files.isDirectory(targetPath)) {
                throw new InvalidRequestException(
                        "Path '" + relativeViewPath(targetPath).toString() + "' is a directory, not a file");
            }
            downloadFile(path, output);
        }
    }

    public void deleteEntry(Path path) {
        logger.info("Deleting entry at path '" + path.toString() + "' from stash with code '" + code + "'");
        refreshAndVerifyNotExpired();
        verifyNotReadOnly();

        if (path.equals(Path.of("/"))) {
            throw new InvalidRequestException("Cannot delete root directory");
        }

        ValidationPatterns.verifyAbsolutePath(path.toString());
        Path targetPath = ValidationPatterns.verifyInsideStash(stashPath, path.toString());

        if (!Files.exists(targetPath)) {
            throw new ResourceNotFoundException(relativeViewPath(targetPath).toString());
        }

        try {
            Files.walkFileTree(targetPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    meta.nodeCategories().remove(relativeViewPath(file).toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                        throws IOException {
                    meta.nodeCategories().remove(relativeViewPath(dir).toString());
                    return FileVisitResult.CONTINUE;
                }
            });
            FileUtils.deleteDirectory(targetPath);
            meta.nodeCategories().remove(relativeViewPath(targetPath).toString());

            saveMeta();
        } catch (Exception e) {
            logger.severe("Failed to delete entry: " + e.getMessage());
            throw new ServerResourceException("Failed to delete entry", e);
        }
    }

    public void renameEntry(Path oldPath, Path newPath) {
        logger.info("Renaming entry at path '" + oldPath.toString() + "' to '" + newPath.toString()
                + "' in stash with code '" + code
                + "'");
        refreshAndVerifyNotExpired();
        verifyNotReadOnly();

        if (oldPath.equals(Path.of("/"))) {
            throw new InvalidRequestException("Cannot rename root directory");
        }
        ValidationPatterns.verifyAbsolutePath(oldPath.toString());
        ValidationPatterns.verifyAbsolutePath(newPath.toString());
        Path targetOldPath = ValidationPatterns.verifyInsideStash(stashPath, oldPath.toString());
        Path targetNewPath = ValidationPatterns.verifyInsideStash(stashPath, newPath.toString());

        if (!Files.exists(targetOldPath)) {
            throw new ResourceNotFoundException(relativeViewPath(targetOldPath).toString());
        }
        if (Files.exists(targetNewPath)) {
            throw new ResourceAlreadyExistsException(relativeViewPath(targetNewPath).toString());
        }

        try {
            NodeCategory category = getNodeCategory(oldPath);
            meta.nodeCategories().remove(oldPath.toString());
            meta.nodeCategories().put(newPath.toString(), category);

            // Update categories of all nested entries if it's a directory.
            if (category == NodeCategory.DIRECTORY) {
                Files.walkFileTree(targetOldPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        Path newFilePath = targetNewPath.resolve(targetOldPath.relativize(file)).normalize();
                        Path relativeOldPath = relativeViewPath(file);
                        Path relativeNewPath = relativeViewPath(newFilePath);

                        NodeCategory fileCategory = getNodeCategory(relativeOldPath);
                        meta.nodeCategories().remove(relativeOldPath.toString());
                        meta.nodeCategories().put(relativeNewPath.toString(), fileCategory);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                            throws IOException {
                        Path newDirPath = targetNewPath.resolve(targetOldPath.relativize(dir)).normalize();
                        Path relativeOldPath = relativeViewPath(dir);
                        Path relativeNewPath = relativeViewPath(newDirPath);

                        NodeCategory dirCategory = getNodeCategory(relativeOldPath);
                        meta.nodeCategories().remove(relativeOldPath.toString());
                        meta.nodeCategories().put(relativeNewPath.toString(), dirCategory);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            logger.fine("Moving entry from '" + targetOldPath.toString() + "' to '" + targetNewPath.toString() + "'");
            FileUtils.copyDirectory(targetOldPath, targetNewPath);
            FileUtils.deleteDirectory(targetOldPath);

            saveMeta();
        } catch (Exception e) {
            logger.severe("Failed to rename entry: " + e.getMessage());
            throw new ServerResourceException("Failed to rename entry", e);
        }
    }

    public void createEmptyDirectory(Path parentPath, String name) {
        logger.info("Creating directory in stash with code '" + code + "' at path '" + parentPath.toString()
                + "' with name '" + name + "'");
        refreshAndVerifyNotExpired();
        verifyNotReadOnly();

        ValidationPatterns.verifyAbsolutePath(parentPath.toString());
        Path targetPath = ValidationPatterns.verifyInsideStash(stashPath, parentPath.resolve(name).toString());

        if (Files.exists(targetPath)) {
            throw new ResourceAlreadyExistsException(relativeViewPath(targetPath).toString());
        }

        try {
            Files.createDirectories(targetPath);
            setNodeCategory(relativeViewPath(targetPath), NodeCategory.DIRECTORY);
            saveMeta();
        } catch (Exception e) {
            logger.severe("Failed to create directory: " + e.getMessage());
            throw new ServerResourceException("Failed to create directory", e);
        }
    }

    public void setCategory(Path path, NodeCategory category) {
        logger.info("Setting category of entry at path '" + path.toString() + "' to '" + category.toString()
                + "' in stash with code '" + code + "'");
        refreshAndVerifyNotExpired();
        verifyNotReadOnly();

        ValidationPatterns.verifyAbsolutePath(path.toString());
        Path targetPath = ValidationPatterns.verifyInsideStash(stashPath, path.toString());

        if (!Files.exists(targetPath)) {
            throw new ResourceNotFoundException(relativeViewPath(targetPath).toString());
        }
        if (Files.isDirectory(targetPath) && category != NodeCategory.DIRECTORY) {
            throw new InvalidRequestException(
                    "Cannot set category of a directory to '" + category.toString() + "'");
        }

        try {
            setNodeCategory(relativeViewPath(targetPath), category);
            saveMeta();
        } catch (Exception e) {
            logger.severe("Failed to set category: " + e.getMessage());
            throw new ServerResourceException("Failed to set category", e);
        }
    }
}
