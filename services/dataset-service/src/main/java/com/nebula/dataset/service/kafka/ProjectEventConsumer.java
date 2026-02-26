package com.nebula.dataset.service.kafka;

import com.nebula.catalog.event.avro.ProjectDeleted;
import com.nebula.dataset.service.DatasetManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectEventConsumer {

  private final DatasetManagerService datasetManagerService;

  @KafkaListener(topics = "catalog.events", groupId = "dataset-service-group")
  public void consumeProjectDeleted(ProjectDeleted event) {
    log.info("Received ProjectDeleted event for projectId: {}", event.getProjectId());

    // In a real implementation we would want to implement a retry mechanism or DLQ
    // here.
    // For now, we will cascade delete datasets for this project.
    try {
      datasetManagerService.deleteDatasetsForProject(event.getProjectId().toString());
    } catch (Exception e) {
      log.error("Failed to process ProjectDeleted event for projectId: {}", event.getProjectId(), e);
      throw e; // throw so Kafka listener retries
    }
  }
}
