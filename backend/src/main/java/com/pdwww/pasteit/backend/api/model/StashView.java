package com.pdwww.pasteit.backend.api.model;

import java.nio.file.Path;

public record StashView(
                String code,
                StashMeta meta,
                StashNode rootNode) {
        public static final Path ROOT_PATH = Path.of("/");
}
