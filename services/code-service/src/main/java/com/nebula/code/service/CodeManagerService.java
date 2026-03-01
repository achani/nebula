package com.nebula.code.service;

import com.nebula.code.domain.IdeSession;
import com.nebula.code.domain.IdeSessionRepository;
import com.nebula.code.domain.Repository;
import com.nebula.code.domain.RepositoryRepository;
import com.nebula.code.service.kafka.RepositoryEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeManagerService {

  private final RepositoryRepository repositoryRepository;
  private final IdeSessionRepository ideSessionRepository;
  private final GitManager gitManager;
  private final IdeOrchestrator ideOrchestrator;
  private final RepositoryEventProducer eventProducer;

  @Transactional
  public Repository createRepository(UUID projectId, String name, String description) {
    if (repositoryRepository.findByProjectIdAndName(projectId, name).isPresent()) {
      throw new IllegalArgumentException("Repository already exists: " + name);
    }

    Repository repo = Repository.builder()
        .projectId(projectId)
        .name(name)
        .description(description)
        .build();

    Repository savedRepo = repositoryRepository.save(repo);

    // Initialize bare git repository on disk
    gitManager.initializeBareRepository(savedRepo.getId());
    log.info("Initialized bare Git repository for ID: {}", savedRepo.getId());

    // Publish event so Catalog service displays this as an Item
    eventProducer.publishRepositoryCreatedEvent(savedRepo);

    return savedRepo;
  }

  public List<Repository> listRepositories(UUID projectId) {
    return repositoryRepository.findByProjectId(projectId);
  }

  public Repository getRepository(UUID id) {
    return repositoryRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + id));
  }

  @Transactional
  public IdeSession launchIde(UUID repositoryId) {
    Repository repo = getRepository(repositoryId);

    // Check if an active session already exists
    return ideSessionRepository.findByRepositoryIdAndStatus(repositoryId, IdeSession.IdeSessionStatus.RUNNING)
        .orElseGet(() -> {
          // Ensure a non-bare workspace exists for the IDE
          gitManager.ensureWorkspace(repositoryId);
          File workspaceDir = gitManager.getWorkspaceDirectory(repositoryId);

          IdeSession newSession = ideOrchestrator.provisionIde(repo, workspaceDir);
          return ideSessionRepository.save(newSession);
        });
  }

  @Transactional
  public void stopIde(UUID sessionId) {
    IdeSession session = ideSessionRepository.findById(sessionId)
        .orElseThrow(() -> new IllegalArgumentException("Session not found"));

    ideOrchestrator.stopIde(session);
    session.setStatus(IdeSession.IdeSessionStatus.STOPPED);
    ideSessionRepository.save(session);
  }
}
