package com.nebula.code.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CodeDto {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RepositoryCreateRequest {
    @NotNull
    private UUID projectId;
    @NotBlank
    private String name;
    private String description;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RepositoryResponse {
    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private String defaultBranch;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class IdeSessionResponse {
    private UUID id;
    private UUID repositoryId;
    private String containerId;
    private String status;
    private String proxyUrl;
  }
}
