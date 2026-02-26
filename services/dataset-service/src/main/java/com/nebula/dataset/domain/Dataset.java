package com.nebula.dataset.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "dataset", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "project_id", "name" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dataset {

  @Id
  @Column(length = 36, updatable = false, nullable = false)
  private String id;

  @Column(name = "project_id", length = 36, nullable = false)
  private String projectId;

  @Column(nullable = false)
  private String name;

  private String description;

  @Enumerated(EnumType.STRING)
  @Column(length = 50, nullable = false)
  private DatasetFormat format;

  @Column(name = "storage_path", length = 1024, nullable = false)
  private String storagePath;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
}
