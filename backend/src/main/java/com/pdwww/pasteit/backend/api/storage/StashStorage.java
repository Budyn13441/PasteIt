package com.pdwww.pasteit.backend.api.storage;

import java.io.File;
import java.io.IOException;
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.pdwww.pasteit.backend.api.exception.InvalidRequestException;
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
        if (!code.matches(ValidationPatterns.STASH_CODE_REGEX)) {
            throw new InvalidRequestException(code);
        }

        Path stashPath = ROOT_DIR.resolve(code);
        Path metaPath = META_DIR.resolve(code + ".json");

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
        if (!code.matches(ValidationPatterns.STASH_CODE_REGEX)) {
            throw new InvalidRequestException(code);
        }

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
        Path stashPath = ROOT_DIR.resolve(code);
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

    private void syncMeta() {
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
        return StashView.ROOT_PATH.resolve(stashPath.relativize(absolutePath));
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
        syncMeta();
    }

    public void makeReadOnly() {
        refreshAndVerifyNotExpired();
        verifyNotReadOnly();
        meta = new StashMeta(meta.expirationDate(), true, meta.nodeCategories());
        syncMeta();
    }
}
