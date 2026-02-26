package com.nebula.catalog.repository;

import com.nebula.catalog.domain.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, UUID> {
  List<CatalogItem> findByProjectIdAndParentIsNull(UUID projectId);

  List<CatalogItem> findByParentId(UUID parentId);

  Optional<CatalogItem> findByProjectIdAndNameAndParentIsNull(UUID projectId, String name);

  Optional<CatalogItem> findByParentIdAndName(UUID parentId, String name);
}
