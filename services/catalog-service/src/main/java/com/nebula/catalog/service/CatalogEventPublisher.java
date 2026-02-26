package com.nebula.catalog.service;

import com.nebula.catalog.event.avro.FolderCreated;
import com.nebula.catalog.event.avro.ProjectCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private static final String TOPIC = "catalog.events";

  public void publishProjectCreated(ProjectCreated event) {
    log.info("Publishing ProjectCreated event for project: {}", event.getId());
    kafkaTemplate.send(TOPIC, event.getId().toString(), event);
  }

  public void publishFolderCreated(FolderCreated event) {
    log.info("Publishing FolderCreated event for folder: {}", event.getId());
    kafkaTemplate.send(TOPIC, event.getId().toString(), event);
  }
}
