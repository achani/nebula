package com.nebula.dataset.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, String> {
  Optional<Dataset> findByProjectIdAndName(String projectId, String name);
}
