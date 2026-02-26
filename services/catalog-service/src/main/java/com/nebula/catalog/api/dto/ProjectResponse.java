package com.nebula.catalog.api.dto;

import com.nebula.catalog.domain.Project;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    String name,
    String description,
    String createdBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {
  public static ProjectResponse fromEntity(Project project) {
    return new ProjectResponse(
        project.getId(),
        project.getName(),
        project.getDescription(),
        project.getCreatedBy(),
        project.getCreatedAt(),
        project.getUpdatedAt());
  }
}
