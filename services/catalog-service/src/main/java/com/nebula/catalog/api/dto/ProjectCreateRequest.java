package com.nebula.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectCreateRequest(
    @NotBlank(message = "Project name cannot be blank") String name,

    String description) {
}
