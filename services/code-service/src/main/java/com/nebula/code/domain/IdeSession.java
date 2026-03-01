package com.nebula.code.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ide_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdeSession {

  @Id
  @Builder.Default
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "repository_id", nullable = false)
  private Repository repository;

  @Column(name = "container_id", nullable = false)
  private String containerId;

  @Column(nullable = false)
  private Integer port;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private IdeSessionStatus status;

  @Column(name = "last_accessed_at")
  private Instant lastAccessedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public enum IdeSessionStatus {
    STARTING, RUNNING, STOPPED, ERROR
  }
}
