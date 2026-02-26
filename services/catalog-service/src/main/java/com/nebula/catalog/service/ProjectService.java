package com.nebula.catalog.service;

import com.nebula.catalog.api.dto.ProjectCreateRequest;
import com.nebula.catalog.api.dto.ProjectResponse;
import com.nebula.catalog.domain.Project;
import com.nebula.catalog.exception.DuplicateResourceException;
import com.nebula.catalog.exception.ResourceNotFoundException;
import com.nebula.catalog.exception.ForbiddenException;
import com.nebula.catalog.infrastructure.AuthPolicyClient;
import com.nebula.catalog.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final CatalogEventPublisher eventPublisher;
  private final AuthPolicyClient authPolicyClient;

  @Transactional
  public ProjectResponse createProject(ProjectCreateRequest request, String username) {
    // Note: In a real app the roles would come from the JWT Context Holder.
    // We are passing a hardcoded role here just to prove the OPA Rego integration
    // works.
    if (!authPolicyClient.authorize(username, Collections.singletonList("nebula-user"), "create_project",
        "project")) {
      throw new ForbiddenException("User is not authorized to create projects");
    }

    if (projectRepository.existsByName(request.name())) {
      throw new DuplicateResourceException("Project with name '" + request.name() + "' already exists");
    }

    Project project = new Project();
    project.setName(request.name());
    project.setDescription(request.description());
    project.setCreatedBy(username);

    Project saved = projectRepository.save(project);

    com.nebula.catalog.event.avro.ProjectCreated event = com.nebula.catalog.event.avro.ProjectCreated.newBuilder()
        .setId(saved.getId().toString())
        .setName(saved.getName())
        .setCreatedBy(saved.getCreatedBy())
        .setTimestamp(saved.getCreatedAt().toInstant())
        .build();

    eventPublisher.publishProjectCreated(event);

    return ProjectResponse.fromEntity(saved);
  }

  @Transactional(readOnly = true)
  public List<ProjectResponse> getAllProjects() {
    return projectRepository.findAll().stream()
        .map(ProjectResponse::fromEntity)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public ProjectResponse getProject(UUID id) {
    return projectRepository.findById(id)
        .map(ProjectResponse::fromEntity)
        .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
  }
}
