package com.nebula.dataset.api.dto;

import com.nebula.dataset.domain.DatasetVersion;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class DatasetVersionResponse {
  private String id;
  private String datasetId;
  private Long versionNumber;
  private OffsetDateTime commitTimestamp;
  private String operation;
  private String operationParameters;

  public static DatasetVersionResponse fromEntity(DatasetVersion version) {
    return DatasetVersionResponse.builder()
        .id(version.getId())
        .datasetId(version.getDataset().getId())
        .versionNumber(version.getVersionNumber())
        .commitTimestamp(version.getCommitTimestamp())
        .operation(version.getOperation())
        .operationParameters(version.getOperationParameters())
        .build();
  }
}
