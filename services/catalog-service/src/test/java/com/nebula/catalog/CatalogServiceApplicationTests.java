package com.nebula.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {
    // Disable config server lookup for tests
    "spring.cloud.config.enabled=false",
    // Use random port for gRPC server
    "grpc.server.port=0"
})
@Testcontainers
class CatalogServiceApplicationTests {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

  @Container
  static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.url", postgres::getJdbcUrl);
    registry.add("spring.flyway.user", postgres::getUsername);
    registry.add("spring.flyway.password", postgres::getPassword);
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    // Schema registry is not easily containerized standalone for simple tests
    // without zookeeper in testcontainers natively,
    // so we disable the actual Avro serialization in the context load test or mock
    // it.
    // For ContextLoads test, simple properties are enough to pass if we don't send
    // active messages.
  }

  @Test
  void contextLoads() {
    // Verifies that the Spring application context starts up correctly,
    // including Flyway migrations and JPA entity scanning
  }
}
