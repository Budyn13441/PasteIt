package com.pdwww.pasteit.backend.api.mapper;

import org.springframework.stereotype.Service;

import com.pdwww.pasteit.backend.api.dto.common.FileCategoryDto;
import com.pdwww.pasteit.backend.api.dto.response.FileContentViewDto;
import com.pdwww.pasteit.backend.api.dto.response.FileTreeNodeViewDto;
import com.pdwww.pasteit.backend.api.dto.response.StashViewResponseDto;
import com.pdwww.pasteit.backend.api.model.NodeCategory;
import com.pdwww.pasteit.backend.api.model.StashNode;
import com.pdwww.pasteit.backend.api.model.StashView;

@Service
public class Mapper {
    public StashViewResponseDto toStashViewResponseDto(StashView view) {
        return new StashViewResponseDto(view.meta().expirationDate().toOffsetDateTime(), view.meta().readOnly(),
                toFileTreeNodeViewDto(view.rootNode()));
    }

    public FileTreeNodeViewDto toFileTreeNodeViewDto(StashNode node) {
        return new FileTreeNodeViewDto(node.getPath().toString(), node.getName(),
                node.getCategory() == NodeCategory.DIRECTORY,
                toFileContentViewDto(node),
                node.getCategory() == NodeCategory.DIRECTORY
                        ? node.getChildren().stream().map(this::toFileTreeNodeViewDto).toList()
                        : null);
    }

    public FileContentViewDto toFileContentViewDto(StashNode node) {
        if (node.getCategory() == NodeCategory.DIRECTORY) {
            return null;
        }
        return new FileContentViewDto(node.getName(), node.getSize(), switch (node.getCategory()) {
            case TEXT -> FileCategoryDto.TEXT;
            case IMAGE -> FileCategoryDto.IMAGE;
            default -> FileCategoryDto.OTHER;
        });
    }
}
