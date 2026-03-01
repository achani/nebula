package com.nebula.code.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositoryRepository extends JpaRepository<com.nebula.code.domain.Repository, UUID> {
  List<com.nebula.code.domain.Repository> findByProjectId(UUID projectId);

  Optional<com.nebula.code.domain.Repository> findByProjectIdAndName(UUID projectId, String name);
}
