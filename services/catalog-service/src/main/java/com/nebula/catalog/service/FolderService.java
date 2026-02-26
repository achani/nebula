package com.nebula.catalog.service;

import com.nebula.catalog.api.dto.FolderCreateRequest;
import com.nebula.catalog.api.dto.FolderResponse;
import com.nebula.catalog.domain.CatalogItem;
import com.nebula.catalog.domain.Folder;
import com.nebula.catalog.domain.Project;
import com.nebula.catalog.exception.DuplicateResourceException;
import com.nebula.catalog.exception.ResourceNotFoundException;
import com.nebula.catalog.infrastructure.AuthPolicyClient;
import com.nebula.catalog.repository.CatalogItemRepository;
import com.nebula.catalog.repository.FolderRepository;
import com.nebula.catalog.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.nebula.catalog.exception.ForbiddenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderService {

  private final FolderRepository folderRepository;
  private final ProjectRepository projectRepository;
  private final CatalogItemRepository catalogItemRepository;
  private final CatalogEventPublisher eventPublisher;
  private final AuthPolicyClient authPolicyClient;

  @Transactional
  public FolderResponse createFolder(UUID projectId, FolderCreateRequest request, String username) {
    // Note: Same as ProjectService, hardcoding roles for the MVP integration
    // pattern
    if (!authPolicyClient.authorize(username, Collections.singletonList("nebula-user"), "create_folder", "folder")) {
      throw new ForbiddenException("User is not authorized to create folders");
    }

    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

    Folder folder = new Folder();
    folder.setProject(project);
    folder.setName(request.name());
    folder.setCreatedBy(username);

    if (request.parentId() != null) {
      CatalogItem parent = catalogItemRepository.findById(request.parentId())
          .orElseThrow(() -> new ResourceNotFoundException("Parent catalog item not found: " + request.parentId()));

      if (!parent.getProject().getId().equals(projectId)) {
        throw new IllegalArgumentException("Parent item does not belong to the specified project");
      }

      if (catalogItemRepository.findByParentIdAndName(request.parentId(), request.name()).isPresent()) {
        throw new DuplicateResourceException(
            "Folder with name '" + request.name() + "' already exists in this parent folder");
      }
      folder.setParent(parent);
    } else {
      if (catalogItemRepository.findByProjectIdAndNameAndParentIsNull(projectId, request.name()).isPresent()) {
        throw new DuplicateResourceException(
            "Folder with name '" + request.name() + "' already exists at the project root");
      }
    }

    Folder saved = folderRepository.save(folder);

    com.nebula.catalog.event.avro.FolderCreated event = com.nebula.catalog.event.avro.FolderCreated.newBuilder()
        .setId(saved.getId().toString())
        .setProjectId(saved.getProject().getId().toString())
        .setParentId(saved.getParent() != null ? saved.getParent().getId().toString() : null)
        .setName(saved.getName())
        .setCreatedBy(saved.getCreatedBy())
        .setTimestamp(saved.getCreatedAt().toInstant())
        .build();

    eventPublisher.publishFolderCreated(event);

    return FolderResponse.fromEntity(saved);
  }

  @Transactional(readOnly = true)
  public List<FolderResponse> listFolders(UUID projectId, UUID parentId) {
    List<CatalogItem> items;
    if (parentId != null) {
      items = catalogItemRepository.findByParentId(parentId);
    } else {
      items = catalogItemRepository.findByProjectIdAndParentIsNull(projectId);
    }

    return items.stream()
        .filter(item -> item instanceof Folder)
        .map(item -> FolderResponse.fromEntity((Folder) item))
        .collect(Collectors.toList());
  }
}
