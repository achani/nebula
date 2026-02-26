# ADR 002: Istio as Service Mesh

**Date:** 2026-02-26  
**Status:** Accepted  

## Context
Nebula involves multiple microservices communicating synchronously (gRPC/REST) and requiring strict security, resilience, and observability. Historically in the Spring ecosystem, this was handled by client-side libraries: **Eureka** (Discovery), **Ribbon/Spring Cloud LoadBalancer** (Load Balancing), and **Hystrix/Resilience4j** (Circuit Breaking).

## Decision
We will use **Istio on Kubernetes** as our service mesh instead of embedding client-side application libraries for service discovery and traffic routing.

## Rationale
1. **Language Agnostic:** While Nebula backend services are Java/Spring Boot, moving routing, load balancing, and mTLS to the infrastructure layer (the Envoy sidecar proxy) allows us to introduce other languages (e.g., Python for ML, Go for tooling) in the future without porting Eureka/Ribbon clients.
2. **Transparent mTLS:** Security is paramount. Istio enforces mutual TLS (mTLS) between all pods transparently. Developers don't manage Java Keystores or certificates in the application code.
3. **Observability for Free:** The Envoy proxy emits detailed metrics (Latency, Error Rates, Traffic volume) and propagates distributed traces without requiring extensive application-level instrumentation for network calls.
4. **Resilience:** Istio policies handle retries, timeouts, and circuit breaking at the network layer. (Note: We will still use Resilience4j in the application code for business-logic fallbacks, but Istio acts as a strong first line of defense).

## Consequences
- **Positive:** Cleaner business logic in the Spring Boot services. Infrastructure concerns are isolated to Kubernetes manifests (`VirtualService`, `DestinationRule`, `PeerAuthentication`).
- **Negative:** Increased complexity in the Kubernetes environment. Developers need to understand Istio telemetry and routing rules.
- **Mitigation:** Start with simple configurations (STRICT mTLS and basic Ingress Gateways). Avoid complex traffic shifting rules until necessary. Local development can still rely on basic docker-compose networking without Istio for inner-loop speed.
