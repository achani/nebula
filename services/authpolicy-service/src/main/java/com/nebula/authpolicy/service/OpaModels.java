package com.nebula.authpolicy.service;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class OpaModels {

  @Data
  @NoArgsConstructor
  public static class OpaInput {
    private User user;
    private String action;
    private String resource;
    private Map<String, String> context;
  }

  @Data
  @NoArgsConstructor
  public static class User {
    private String id;
    private List<String> roles;
  }

  @Data
  @NoArgsConstructor
  public static class OpaRequest {
    private OpaInput input;
  }

  @Data
  @NoArgsConstructor
  public static class OpaResponse {
    private OpaResult result;
  }

  @Data
  @NoArgsConstructor
  public static class OpaResult {
    private boolean allow;
    private String reason;
  }
}
