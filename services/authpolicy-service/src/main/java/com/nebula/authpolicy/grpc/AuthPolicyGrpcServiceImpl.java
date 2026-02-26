package com.nebula.authpolicy.grpc;

import com.nebula.authpolicy.service.OpaEvaluatorService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class AuthPolicyGrpcServiceImpl extends AuthPolicyServiceGrpc.AuthPolicyServiceImplBase {

  private static final Logger log = LoggerFactory.getLogger(AuthPolicyGrpcServiceImpl.class);
  private final OpaEvaluatorService opaEvaluatorService;

  public AuthPolicyGrpcServiceImpl(OpaEvaluatorService opaEvaluatorService) {
    this.opaEvaluatorService = opaEvaluatorService;
  }

  @Override
  public void authorize(AuthorizeRequest request, StreamObserver<AuthorizeResponse> responseObserver) {
    log.info("Received authorization request: User={}, Action={}, Resource={}",
        request.getUserId(), request.getAction(), request.getResource());

    AuthorizeResponse response = opaEvaluatorService.evaluate(request);

    log.info("Authorization result: Allowed={}, Reason={}", response.getAllowed(), response.getReason());

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
