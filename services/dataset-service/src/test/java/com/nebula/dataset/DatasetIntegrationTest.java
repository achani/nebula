package com.nebula.dataset;

import com.nebula.dataset.api.dto.DatasetCreateRequest;
import com.nebula.dataset.api.dto.DatasetResponse;
import com.nebula.dataset.domain.DatasetFormat;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=",
    // Mock AuthPolicy via properties or just let it fail/mock it
    // We will disable security/authz for simplicity in this basic setup
    "grpc.client.authpolicy-service.address=static://localhost:9090"
})
class DatasetIntegrationTest {

  @LocalServerPort
  private int port;

  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

  @Container
  static final MinIOContainer minio = new MinIOContainer("minio/minio:RELEASE.2023-09-07T02-05-02Z")
      .withUserName("minioadmin")
      .withPassword("minioadmin");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    registry.add("minio.endpoint", minio::getS3URL);
    registry.add("spark.hadoop.fs.s3a.endpoint", minio::getS3URL);
  }

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
  }

  // A simple sanity check test. In a real scenario, we'd mock the
  // AuthPolicyClient
  // to return true so we can test the actual controller logic fully.
  @Test
  void contextLoads() {
    // Just verify context starts up with all containers
  }
}
