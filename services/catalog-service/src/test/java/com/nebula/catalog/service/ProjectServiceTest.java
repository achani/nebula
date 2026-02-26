package com.nebula.catalog.service;

import com.nebula.catalog.api.dto.ProjectCreateRequest;
import com.nebula.catalog.api.dto.ProjectResponse;
import com.nebula.catalog.domain.Project;
import com.nebula.catalog.exception.DuplicateResourceException;
import com.nebula.catalog.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private CatalogEventPublisher eventPublisher;

  @InjectMocks
  private ProjectService projectService;

  @Test
  void createProject_Success() {
    ProjectCreateRequest request = new ProjectCreateRequest("Test Project", "Description");
    when(projectRepository.existsByName("Test Project")).thenReturn(false);

    Project savedProject = new Project();
    savedProject.setId(UUID.randomUUID());
    savedProject.setName("Test Project");
    savedProject.setCreatedBy("admin");
    savedProject.setCreatedAt(OffsetDateTime.now());
    savedProject.setUpdatedAt(OffsetDateTime.now());

    when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

    ProjectResponse response = projectService.createProject(request, "admin");

    assertNotNull(response);
    assertEquals("Test Project", response.name());
    verify(eventPublisher, times(1)).publishProjectCreated(any());
  }

  @Test
  void createProject_DuplicateName_ThrowsException() {
    ProjectCreateRequest request = new ProjectCreateRequest("Existing Project", "Description");
    when(projectRepository.existsByName("Existing Project")).thenReturn(true);

    assertThrows(DuplicateResourceException.class, () -> {
      projectService.createProject(request, "admin");
    });

    verify(projectRepository, never()).save(any());
    verify(eventPublisher, never()).publishProjectCreated(any());
  }
}
