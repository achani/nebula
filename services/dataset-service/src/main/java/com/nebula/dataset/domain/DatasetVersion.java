package com.nebula.dataset.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "dataset_version", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "dataset_id", "version_number" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetVersion {

  @Id
  @Column(length = 36, updatable = false, nullable = false)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dataset_id", nullable = false)
  private Dataset dataset;

  @Column(name = "version_number", nullable = false)
  private Long versionNumber;

  @Column(name = "commit_timestamp", nullable = false)
  private OffsetDateTime commitTimestamp;

  private String operation;

  @Column(name = "operation_parameters", columnDefinition = "jsonb")
  private String operationParameters; // JSON string
}
