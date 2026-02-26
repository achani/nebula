package com.nebula.catalog.api;

import com.nebula.catalog.api.dto.FolderCreateRequest;
import com.nebula.catalog.api.dto.FolderResponse;
import com.nebula.catalog.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/projects/{projectId}/folders")
@RequiredArgsConstructor
public class FolderController {

  private final FolderService folderService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FolderResponse createFolder(@PathVariable UUID projectId,
      @RequestBody @Valid FolderCreateRequest request,
      @RequestHeader(value = "X-Forwarded-User", defaultValue = "system") String username) {
    return folderService.createFolder(projectId, request, username);
  }

  @GetMapping
  public List<FolderResponse> listFolders(@PathVariable UUID projectId,
      @RequestParam(required = false) UUID parentId) {
    return folderService.listFolders(projectId, parentId);
  }
}
