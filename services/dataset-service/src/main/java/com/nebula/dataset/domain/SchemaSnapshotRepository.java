package com.nebula.dataset.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchemaSnapshotRepository extends JpaRepository<SchemaSnapshot, String> {
  Optional<SchemaSnapshot> findFirstByDatasetIdOrderByVersionNumberDesc(String datasetId);

  Optional<SchemaSnapshot> findByDatasetIdAndVersionNumber(String datasetId, Long versionNumber);
}
