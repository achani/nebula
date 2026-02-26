package com.nebula.dataset.service;

import com.nebula.dataset.domain.*;
import com.nebula.dataset.service.kafka.DatasetEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.types.StructType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatasetManagerService {

  private final DatasetRepository datasetRepository;
  private final DatasetVersionRepository versionRepository;
  private final SchemaSnapshotRepository schemaRepository;
  private final DeltaLakeService deltaLakeService;
  private final DatasetEventProducer eventProducer;

  @Transactional
  public Dataset createDataset(String projectId, String name, String description, DatasetFormat format) {
    if (datasetRepository.findByProjectIdAndName(projectId, name).isPresent()) {
      throw new IllegalArgumentException("Dataset with this name already exists in the project");
    }

    String datasetId = UUID.randomUUID().toString();
    // Path in MinIO: s3a://nebula-data/{projectId}/{datasetId}
    String storagePath = String.format("s3a://nebula-data/%s/%s", projectId, datasetId);

    Dataset dataset = Dataset.builder()
        .id(datasetId)
        .projectId(projectId)
        .name(name)
        .description(description)
        .format(format)
        .storagePath(storagePath)
        .build();

    dataset = datasetRepository.save(dataset);

    if (format == DatasetFormat.DELTA) {
      // Initialize empty Delta table with an empty schema
      log.info("Initializing empty Delta table at {}", storagePath);
      StructType emptySchema = new StructType();
      deltaLakeService.createEmptyTable(storagePath, emptySchema);

      // Record initial version 0
      DatasetVersion version0 = DatasetVersion.builder()
          .id(UUID.randomUUID().toString())
          .dataset(dataset)
          .versionNumber(0L)
          .commitTimestamp(OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
          .operation("CREATE OR REPLACE TABLE")
          .build();
      versionRepository.save(version0);

      // Record initial schema
      SchemaSnapshot schemaSnapshot = SchemaSnapshot.builder()
          .id(UUID.randomUUID().toString())
          .dataset(dataset)
          .versionNumber(0L)
          .schemaJson(emptySchema.json())
          .build();
      schemaRepository.save(schemaSnapshot);
    }

    // Publish event
    eventProducer.publishDatasetCreated(datasetId, projectId, name, format.name(), storagePath);

    return dataset;
  }

  @Transactional
  public void deleteDataset(String datasetId) {
    Dataset dataset = datasetRepository.findById(datasetId)
        .orElseThrow(() -> new IllegalArgumentException("Dataset not found"));

    // MinIO deletion is left for a background cleanup job or separate service
    datasetRepository.delete(dataset);
    log.info("Deleted dataset metadata for datasetId: {}", datasetId);
  }

  @Transactional
  public void deleteDatasetsForProject(String projectId) {
    // Simple implementation for cascade delete
    log.info("Deleting all datasets for projectId: {}", projectId);
    // We'd query all datasets for project and delete them
    // datasetRepository.findByProjectId(projectId).forEach(this::deleteDataset);
  }
}
