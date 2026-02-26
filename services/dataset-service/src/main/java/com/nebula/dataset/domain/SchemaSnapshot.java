package com.nebula.dataset.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "schema_snapshot", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "dataset_id", "version_number" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemaSnapshot {

  @Id
  @Column(length = 36, updatable = false, nullable = false)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dataset_id", nullable = false)
  private Dataset dataset;

  @Column(name = "version_number", nullable = false)
  private Long versionNumber;

  @Column(name = "schema_json", columnDefinition = "jsonb", nullable = false)
  private String schemaJson;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private OffsetDateTime createdAt;
}
