package com.nebula.code.service.kafka;

import com.nebula.code.domain.Repository;
import com.nebula.code.event.avro.RepositoryCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryEventProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private static final String TOPIC_REPOSITORY_EVENTS = "code.events";

  public void publishRepositoryCreatedEvent(Repository repository) {
    RepositoryCreated event = RepositoryCreated.newBuilder()
        .setRepositoryId(repository.getId().toString())
        .setProjectId(repository.getProjectId().toString())
        .setName(repository.getName())
        .setTimestamp(repository.getCreatedAt() != null ? repository.getCreatedAt() : Instant.now())
        .build();

    kafkaTemplate.send(TOPIC_REPOSITORY_EVENTS, repository.getProjectId().toString(), event)
        .whenComplete((result, ex) -> {
          if (ex == null) {
            log.info("Successfully published RepositoryCreated event for repo: {}", repository.getId());
          } else {
            log.error("Failed to publish RepositoryCreated event for repo: {}", repository.getId(), ex);
          }
        });
  }
}
