package com.nebula.gateway.filter;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();

    // Extract existing or generate new Correlation ID
    String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
    if (correlationId == null || correlationId.isEmpty()) {
      correlationId = UUID.randomUUID().toString();
    }

    // Add it to the downstream request headers
    ServerHttpRequest mutatedRequest = request.mutate()
        .header(CORRELATION_ID_HEADER, correlationId)
        .build();

    ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

    // Pass it to the reactive MDC logic for logging
    final String finalCorrelationId = correlationId;
    return chain.filter(mutatedExchange)
        .contextWrite(context -> {
          MDC.put("correlation_id", finalCorrelationId);
          return context.put("correlation_id", finalCorrelationId);
        });
  }

  @Override
  public int getOrder() {
    // Run early in the filter chain
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
