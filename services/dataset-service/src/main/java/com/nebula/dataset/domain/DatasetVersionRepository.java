package com.nebula.dataset.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatasetVersionRepository extends JpaRepository<DatasetVersion, String> {
  List<DatasetVersion> findByDatasetIdOrderByVersionNumberDesc(String datasetId);

  Optional<DatasetVersion> findByDatasetIdAndVersionNumber(String datasetId, Long versionNumber);
}
