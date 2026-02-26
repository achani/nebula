package com.nebula.catalog.api;

import com.nebula.catalog.api.dto.ProjectCreateRequest;
import com.nebula.catalog.api.dto.ProjectResponse;
import com.nebula.catalog.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/projects")
@RequiredArgsConstructor
public class ProjectController {

  private final ProjectService projectService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProjectResponse createProject(@RequestBody @Valid ProjectCreateRequest request,
      @RequestHeader(value = "X-Forwarded-User", defaultValue = "system") String username) {
    return projectService.createProject(request, username);
  }

  @GetMapping
  public List<ProjectResponse> getProjects() {
    return projectService.getAllProjects();
  }

  @GetMapping("/{id}")
  public ProjectResponse getProject(@PathVariable UUID id) {
    return projectService.getProject(id);
  }
}
