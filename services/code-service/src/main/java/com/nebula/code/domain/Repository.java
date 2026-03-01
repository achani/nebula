package com.nebula.code.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "repository")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Repository {

  @Id
  @Builder.Default
  private UUID id = UUID.randomUUID();

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(nullable = false)
  private String name;

  @Column
  private String description;

  @Column(name = "default_branch")
  @Builder.Default
  private String defaultBranch = "main";

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
