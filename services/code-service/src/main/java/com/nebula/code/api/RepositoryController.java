package com.nebula.code.api;

import com.nebula.code.api.dto.CodeDto;
import com.nebula.code.domain.IdeSession;
import com.nebula.code.domain.Repository;
import com.nebula.code.infrastructure.AuthPolicyClient;
import com.nebula.code.service.CodeManagerService;
import com.nebula.code.service.GitManager;
import com.nebula.code.service.IdeOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class RepositoryController {

  private final CodeManagerService codeManagerService;
  private final GitManager gitManager;
  private final IdeOrchestrator ideOrchestrator;
  private final AuthPolicyClient authPolicyClient;

  @PostMapping
  public ResponseEntity<CodeDto.RepositoryResponse> createRepository(
      @RequestHeader(value = "X-Forwarded-User", defaultValue = "system") String username,
      @RequestHeader(value = "X-Forwarded-Roles", defaultValue = "") String rolesHeader,
      @Valid @RequestBody CodeDto.RepositoryCreateRequest request) {

    List<String> roles = rolesHeader.isEmpty() ? List.of() : List.of(rolesHeader.split(","));
    System.out.println("===> createRepository called with username=" + username + ", rolesHeader=" + rolesHeader
        + ", name=" + request.getName());
    // Validate user can edit project
    if (!authPolicyClient.checkPermission(username, roles, "project", request.getProjectId().toString(), "edit")) {
      System.out.println("===> createRepository DENIED for user=" + username);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    Repository repo = codeManagerService.createRepository(request.getProjectId(), request.getName(),
        request.getDescription());
    return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(repo));
  }

  @GetMapping
  public ResponseEntity<List<CodeDto.RepositoryResponse>> listRepositories(
      @RequestHeader(value = "X-Forwarded-User", defaultValue = "system") String username,
      @RequestHeader(value = "X-Forwarded-Roles", defaultValue = "") String rolesHeader,
      @RequestParam UUID projectId) {

    List<String> roles = rolesHeader.isEmpty() ? List.of() : java.util.Arrays.asList(rolesHeader.split(","));
    System.out.println("===> listRepositories called with username=" + username + ", rolesHeader=" + rolesHeader
        + ", parsed roles=" + roles);
    if (!authPolicyClient.checkPermission(username, roles, "project", projectId.toString(), "view")) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    List<CodeDto.RepositoryResponse> responses = codeManagerService.listRepositories(projectId).stream()
        .map(this::mapToDto)
        .collect(Collectors.toList());
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/{id}")
  public ResponseEntity<CodeDto.RepositoryResponse> getRepository(
      @RequestHeader(value = "X-Forwarded-User", defaultValue = "system") String username,
      @RequestHeader(value = "X-Forwarded-Roles", defaultValue = "") String rolesHeader,
      @PathVariable UUID id) {

    Repository repo = codeManagerService.getRepository(id);
    List<String> roles = rolesHeader.isEmpty() ? List.of() : List.of(rolesHeader.split(","));
    if (!authPolicyClient.checkPermission(username, roles, "project", repo.getProjectId().toString(), "view")) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    return ResponseEntity.ok(mapToDto(repo));
  }

  // Git File Browsing
  @GetMapping("/{id}/files")
  public ResponseEntity<List<String>> listFiles(
      @RequestHeader(value = "X-Forwarded-User", defaultValue = "system") String username,
      @RequestHeader(value = "X-Forwarded-Roles", defaultValue = "") String rolesHeader,
      @PathVariable UUID id,
      @RequestParam(defaultValue = "main") String branch) {

    Repository repo = codeManagerService.getRepository(id);
    List<String> roles = rolesHeader.isEmpty() ? List.of() : List.of(rolesHeader.split(","));
    if (!authPolicyClient.checkPermission(username, roles, "project", repo.getProjectId().toString(), "view")) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    return ResponseEntity.ok(gitManager.listFiles(id, branch));
  }

  // IDE Management
  @PostMapping("/{id}/ide/launch")
  public ResponseEntity<CodeDto.IdeSessionResponse> launchIde(
      @RequestHeader(value = "X-Forwarded-User", defaultValue = "system") String username,
      @RequestHeader(value = "X-Forwarded-Roles", defaultValue = "") String rolesHeader,
      @PathVariable UUID id) {

    Repository repo = codeManagerService.getRepository(id);
    List<String> roles = rolesHeader.isEmpty() ? List.of() : List.of(rolesHeader.split(","));
    System.out
        .println("===> launchIde called for repo=" + id + ", username=" + username + ", rolesHeader=" + rolesHeader);
    // Require edit to launch IDE
    if (!authPolicyClient.checkPermission(username, roles, "project", repo.getProjectId().toString(), "edit")) {
      System.out.println("===> launchIde DENIED for user=" + username);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    IdeSession session = codeManagerService.launchIde(id);
    return ResponseEntity.ok(mapToDto(session));
  }

  @PostMapping("/{id}/ide/stop")
  public ResponseEntity<Void> stopIde(
      @RequestHeader(value = "X-Forwarded-User", defaultValue = "system") String username,
      @RequestHeader(value = "X-Forwarded-Roles", defaultValue = "") String rolesHeader,
      @PathVariable UUID id,
      @RequestParam UUID sessionId) {

    Repository repo = codeManagerService.getRepository(id);
    List<String> roles = rolesHeader.isEmpty() ? List.of() : List.of(rolesHeader.split(","));
    if (!authPolicyClient.checkPermission(username, roles, "project", repo.getProjectId().toString(), "edit")) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    codeManagerService.stopIde(sessionId);
    return ResponseEntity.ok().build();
  }

  private CodeDto.RepositoryResponse mapToDto(Repository r) {
    return CodeDto.RepositoryResponse.builder()
        .id(r.getId())
        .projectId(r.getProjectId())
        .name(r.getName())
        .description(r.getDescription())
        .defaultBranch(r.getDefaultBranch())
        .build();
  }

  private CodeDto.IdeSessionResponse mapToDto(IdeSession s) {
    return CodeDto.IdeSessionResponse.builder()
        .id(s.getId())
        .repositoryId(s.getRepository().getId())
        .containerId(s.getContainerId())
        .status(s.getStatus().name())
        .proxyUrl(ideOrchestrator.getIdeProxyUrl(s))
        .build();
  }
}
