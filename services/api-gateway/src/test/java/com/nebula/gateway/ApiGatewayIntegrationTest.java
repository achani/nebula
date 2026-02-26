package com.nebula.gateway;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Testcontainers
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=" // Disable config server import for tests
})
class ApiGatewayIntegrationTest {

  @Container
  static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:24.0.0")
      .withRealmImportFile("nebula-realm.json");

  @DynamicPropertySource
  static void registerResourceServerIssuerProperty(DynamicPropertyRegistry registry) {
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
        () -> keycloak.getAuthServerUrl() + "/realms/nebula");
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        () -> keycloak.getAuthServerUrl() + "/realms/nebula/protocol/openid-connect/certs");
  }

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void testUnauthorizedRequest() {
    webTestClient.get().uri("/api/catalog/ping")
        .exchange()
        .expectStatus().isUnauthorized();
  }
}
