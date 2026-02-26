package com.nebula.dataset.api.dto;

import com.nebula.dataset.domain.Dataset;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class DatasetResponse {
  private String id;
  private String projectId;
  private String name;
  private String description;
  private String format;
  private String storagePath;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  public static DatasetResponse fromEntity(Dataset dataset) {
    return DatasetResponse.builder()
        .id(dataset.getId())
        .projectId(dataset.getProjectId())
        .name(dataset.getName())
        .description(dataset.getDescription())
        .format(dataset.getFormat().name())
        .storagePath(dataset.getStoragePath())
        .createdAt(dataset.getCreatedAt())
        .updatedAt(dataset.getUpdatedAt())
        .build();
  }
}
