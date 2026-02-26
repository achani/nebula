package com.nebula.catalog.api.dto;

import com.nebula.catalog.domain.Folder;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FolderResponse(
    UUID id,
    UUID projectId,
    UUID parentId,
    String name,
    String createdBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {
  public static FolderResponse fromEntity(Folder folder) {
    return new FolderResponse(
        folder.getId(),
        folder.getProject().getId(),
        folder.getParent() != null ? folder.getParent().getId() : null,
        folder.getName(),
        folder.getCreatedBy(),
        folder.getCreatedAt(),
        folder.getUpdatedAt());
  }
}
