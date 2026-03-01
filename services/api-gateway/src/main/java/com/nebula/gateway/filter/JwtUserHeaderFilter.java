package com.nebula.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtUserHeaderFilter implements GlobalFilter, Ordered {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtUserHeaderFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
    System.out.println("==== JwtUserHeaderFilter EXECUTING ====");
    System.out.println("Path: " + exchange.getRequest().getURI());
    System.out.println("AuthHeader exists: " + (authHeader != null));

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      try {
        String token = authHeader.substring(7);
        String[] parts = token.split("\\.");
        if (parts.length >= 2) {
          String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));

          com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
          com.fasterxml.jackson.databind.JsonNode json = mapper.readTree(payload);

          String username = json.path("preferred_username").asText(json.path("sub").asText("system"));

          java.util.List<String> roleList = new java.util.ArrayList<>();
          com.fasterxml.jackson.databind.JsonNode rolesNode = json.path("realm_access").path("roles");
          if (rolesNode.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode roleNode : rolesNode) {
              roleList.add(roleNode.asText());
            }
          }
          String roles = String.join(",", roleList);

          log.info("Parsed identity from header: {}, roles: {}", username, roles);

          ServerHttpRequest request = exchange.getRequest().mutate()
              .header("X-Forwarded-User", username)
              .header("X-Forwarded-Roles", roles)
              .build();
          return chain.filter(exchange.mutate().request(request).build());
        }
      } catch (Exception e) {
        log.error("Failed to parse JWT payload manually", e);
      }
    }

    return chain.filter(exchange);
  }

  @Override
  public int getOrder() {
    return 10000; // Run after Spring Security validation
  }
}
