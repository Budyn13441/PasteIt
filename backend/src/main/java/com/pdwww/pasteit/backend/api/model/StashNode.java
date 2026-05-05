package com.pdwww.pasteit.backend.api.model;

import java.util.List;

import java.nio.file.Path;

public class StashNode {
    private final Path path;
    private final String name;
    private final long size;
    private final NodeCategory category;
    private final List<StashNode> children;

    public Path getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public NodeCategory getCategory() {
        return category;
    }

    public List<StashNode> getChildren() {
        return children;
    }

    public StashNode(Path path, String name, long size, NodeCategory category, List<StashNode> children) {
        this.path = path;
        this.name = name;
        this.size = size;
        this.category = category;
        this.children = children;
    }

    public void addChild(StashNode child) {
        if (category != NodeCategory.DIRECTORY) {
            throw new IllegalStateException("Cannot add child to non-directory node");
        }
        children.add(child);
    }
}
