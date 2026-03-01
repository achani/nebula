package com.nebula.code.infrastructure;

import com.nebula.authpolicy.grpc.AuthorizeRequest;
import com.nebula.authpolicy.grpc.AuthorizeResponse;
import com.nebula.authpolicy.grpc.AuthPolicyServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AuthPolicyClient {

  @GrpcClient("authpolicy-service")
  private AuthPolicyServiceGrpc.AuthPolicyServiceBlockingStub authPolicyStub;

  public boolean checkPermission(String userId, List<String> roles, String resourceType, String resourceId,
      String action) {
    try {
      AuthorizeRequest request = AuthorizeRequest.newBuilder()
          .setUserId(userId)
          .addAllRoles(roles != null ? roles : List.of())
          .setResource(resourceType + ":" + resourceId)
          .setAction(action)
          .build();

      AuthorizeResponse response = authPolicyStub.authorize(request);

      if (!response.getAllowed()) {
        log.warn("Access denied. Reason: {}", response.getReason());
      }

      return response.getAllowed();
    } catch (Exception e) {
      log.error("Failed to check authorization policy via gRPC", e);
      return false; // Fail secure
    }
  }
}
