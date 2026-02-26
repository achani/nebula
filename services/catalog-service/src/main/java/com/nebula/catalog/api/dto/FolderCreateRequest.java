package com.nebula.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record FolderCreateRequest(
    @NotBlank(message = "Folder name cannot be blank") String name,

    // Optional parent folder ID. If null, it's created at the project root.
    UUID parentId) {
}
