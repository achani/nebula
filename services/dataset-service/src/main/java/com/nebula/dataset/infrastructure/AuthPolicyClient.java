package com.nebula.dataset.infrastructure;

import com.nebula.authpolicy.grpc.AuthPolicyServiceGrpc;
import com.nebula.authpolicy.grpc.AuthorizeRequest;
import com.nebula.authpolicy.grpc.AuthorizeResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthPolicyClient {

  private static final Logger log = LoggerFactory.getLogger(AuthPolicyClient.class);

  @GrpcClient("authpolicy-service")
  private AuthPolicyServiceGrpc.AuthPolicyServiceBlockingStub authPolicyStub;

  @CircuitBreaker(name = "authpolicy", fallbackMethod = "authorizeFallback")
  public boolean authorize(String userId, List<String> roles, String action, String resource) {
    AuthorizeRequest request = AuthorizeRequest.newBuilder()
        .setUserId(userId)
        .addAllRoles(roles)
        .setAction(action)
        .setResource(resource)
        .build();

    log.debug("Calling AuthPolicy for User: {}, Action: {}, Resource: {}", userId, action, resource);
    AuthorizeResponse response = authPolicyStub.authorize(request);

    if (!response.getAllowed()) {
      log.warn("AuthPolicy denied action '{}' on resource '{}' for user '{}'. Reason: {}",
          action, resource, userId, response.getReason());
    }

    return response.getAllowed();
  }

  public boolean authorizeFallback(String userId, List<String> roles, String action, String resource, Throwable t) {
    log.error("AuthPolicy Circuit Breaker OPEN or call failed. Failing closed (Deny). " +
        "Action: {}, Resource: {}, User: {}", action, resource, userId, t);
    return false; // Fail closed for security
  }
}
