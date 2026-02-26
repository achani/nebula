package com.nebula.authpolicy.service;

import com.nebula.authpolicy.grpc.AuthorizeRequest;
import com.nebula.authpolicy.grpc.AuthorizeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OpaEvaluatorService {

  private static final Logger log = LoggerFactory.getLogger(OpaEvaluatorService.class);
  private final RestTemplate restTemplate;

  @Value("${opa.url:http://localhost:8181/v1/data/nebula/authz}")
  private String opaEndpoint;

  public OpaEvaluatorService(RestTemplateBuilder builder) {
    this.restTemplate = builder.build();
  }

  public AuthorizeResponse evaluate(AuthorizeRequest grpcRequest) {
    OpaModels.OpaRequest opaRequest = buildOpaRequest(grpcRequest);

    try {
      ResponseEntity<OpaModels.OpaResponse> response = restTemplate.postForEntity(
          opaEndpoint,
          opaRequest,
          OpaModels.OpaResponse.class);

      OpaModels.OpaResult result = response.getBody() != null ? response.getBody().getResult() : null;

      if (result == null) {
        log.warn("OPA returned an empty evaluation result. Denying by default.");
        return AuthorizeResponse.newBuilder()
            .setAllowed(false)
            .setReason("Empty response from OPA engine")
            .build();
      }

      return AuthorizeResponse.newBuilder()
          .setAllowed(result.isAllow())
          .setReason(result.getReason() != null ? result.getReason() : "")
          .build();

    } catch (Exception e) {
      log.error("Failed to evaluate policy against OPA engine at {}", opaEndpoint, e);
      // Default Deny on failure
      return AuthorizeResponse.newBuilder()
          .setAllowed(false)
          .setReason("Internal policy engine failure")
          .build();
    }
  }

  private OpaModels.OpaRequest buildOpaRequest(AuthorizeRequest grpcRequest) {
    OpaModels.User user = new OpaModels.User();
    user.setId(grpcRequest.getUserId());
    user.setRoles(new java.util.ArrayList<>(grpcRequest.getRolesList()));

    OpaModels.OpaInput input = new OpaModels.OpaInput();
    input.setUser(user);
    input.setAction(grpcRequest.getAction());
    input.setResource(grpcRequest.getResource());
    input.setContext(grpcRequest.getContextMap());

    OpaModels.OpaRequest request = new OpaModels.OpaRequest();
    request.setInput(input);
    log.info("Sending OPA Request: {}", request);
    return request;
  }
}
