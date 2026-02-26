package com.nebula.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void shouldInjectNewCorrelationIdWhenNotPresent() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);

    GatewayFilterChain filterChain = serverWebExchange -> {
      String correlationId = serverWebExchange.getRequest().getHeaders()
          .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
      assertNotNull(correlationId);
      assertFalse(correlationId.isEmpty());
      return Mono.empty();
    };

    StepVerifier.create(filter.filter(exchange, filterChain))
        .verifyComplete();
  }

  @Test
  void shouldKeepExistingCorrelationIdWhenPresent() {
    String existingId = "req-12345";
    MockServerHttpRequest request = MockServerHttpRequest.get("/test")
        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId)
        .build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);

    GatewayFilterChain filterChain = serverWebExchange -> {
      String correlationId = serverWebExchange.getRequest().getHeaders()
          .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
      assertEquals(existingId, correlationId);
      return Mono.empty();
    };

    StepVerifier.create(filter.filter(exchange, filterChain))
        .verifyComplete();
  }
}
