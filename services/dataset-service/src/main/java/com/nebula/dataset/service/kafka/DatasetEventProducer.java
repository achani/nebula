package com.nebula.dataset.service.kafka;

import com.nebula.dataset.event.avro.DatasetCreated;
import com.nebula.dataset.event.avro.DatasetUpdated;
import com.nebula.dataset.event.avro.SchemaChanged;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatasetEventProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  private static final String TOPIC_DATASET_EVENTS = "dataset.events";

  public void publishDatasetCreated(String datasetId, String projectId, String name, String format, String path) {
    DatasetCreated event = DatasetCreated.newBuilder()
        .setEventId(UUID.randomUUID().toString())
        .setDatasetId(datasetId)
        .setProjectId(projectId)
        .setName(name)
        .setFormat(format)
        .setPath(path)
        .setTimestamp(Instant.now().toEpochMilli())
        .build();

    log.info("Publishing DatasetCreated event for datasetId: {}", datasetId);
    kafkaTemplate.send(TOPIC_DATASET_EVENTS, datasetId, event);
  }

  public void publishDatasetUpdated(String datasetId, String projectId, long version) {
    DatasetUpdated event = DatasetUpdated.newBuilder()
        .setEventId(UUID.randomUUID().toString())
        .setDatasetId(datasetId)
        .setProjectId(projectId)
        .setVersion(version)
        .setTimestamp(Instant.now().toEpochMilli())
        .build();

    log.info("Publishing DatasetUpdated event for datasetId: {}, version: {}", datasetId, version);
    kafkaTemplate.send(TOPIC_DATASET_EVENTS, datasetId, event);
  }

  public void publishSchemaChanged(String datasetId, String projectId, long version, String schemaJson) {
    SchemaChanged event = SchemaChanged.newBuilder()
        .setEventId(UUID.randomUUID().toString())
        .setDatasetId(datasetId)
        .setProjectId(projectId)
        .setVersion(version)
        .setSchemaJson(schemaJson)
        .setTimestamp(Instant.now().toEpochMilli())
        .build();

    log.info("Publishing SchemaChanged event for datasetId: {}, version: {}", datasetId, version);
    kafkaTemplate.send(TOPIC_DATASET_EVENTS, datasetId, event);
  }
}
