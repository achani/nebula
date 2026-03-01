package com.nebula.code.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdeSessionRepository extends JpaRepository<IdeSession, UUID> {
  Optional<IdeSession> findByRepositoryIdAndStatus(UUID repositoryId, IdeSession.IdeSessionStatus status);

  List<IdeSession> findByStatus(IdeSession.IdeSessionStatus status);
}
