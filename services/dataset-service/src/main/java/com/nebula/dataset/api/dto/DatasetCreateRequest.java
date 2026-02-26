package com.nebula.dataset.api.dto;

import com.nebula.dataset.domain.DatasetFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DatasetCreateRequest {
  @NotBlank
  private String projectId;

  @NotBlank
  private String name;

  private String description;

  @NotNull
  private DatasetFormat format;
}
