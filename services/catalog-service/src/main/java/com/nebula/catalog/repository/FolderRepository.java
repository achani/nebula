package com.nebula.catalog.repository;

import com.nebula.catalog.domain.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FolderRepository extends JpaRepository<Folder, UUID> {
}
