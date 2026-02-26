package com.nebula.dataset.api;

import com.nebula.dataset.api.dto.DatasetCreateRequest;
import com.nebula.dataset.api.dto.DatasetResponse;
import com.nebula.dataset.api.dto.DatasetVersionResponse;
import com.nebula.dataset.domain.Dataset;
import com.nebula.dataset.domain.DatasetRepository;
import com.nebula.dataset.domain.DatasetVersionRepository;
import com.nebula.dataset.infrastructure.AuthPolicyClient;
import com.nebula.dataset.service.DatasetManagerService;
import com.nebula.dataset.service.DeltaLakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
public class DatasetController {

  private final DatasetManagerService datasetManagerService;
  private final DatasetRepository datasetRepository;
  private final DatasetVersionRepository datasetVersionRepository;
  private final AuthPolicyClient authPolicyClient;
  private final DeltaLakeService deltaLakeService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DatasetResponse createDataset(@RequestBody @Valid DatasetCreateRequest request,
      @RequestHeader(value = "X-Forwarded-User", defaultValue = "system") String username) {

    // Authorization check: User must be able to create datasets in the project
    boolean isAllowed = authPolicyClient.authorize(
        username, List.of(), "dataset:create", "project:" + request.getProjectId());

    if (!isAllowed) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to create datasets in this project");
    }

    Dataset dataset = datasetManagerService.createDataset(
        request.getProjectId(),
        request.getName(),
        request.getDescription(),
        request.getFormat());

    return DatasetResponse.fromEntity(dataset);
  }

  @GetMapping("/{id}")
  public DatasetResponse getDataset(@PathVariable String id,
      @RequestHeader(value = "X-Forwarded-User", defaultValue = "system") String username) {

    Dataset dataset = datasetRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset not found"));

    boolean isAllowed = authPolicyClient.authorize(
        username, List.of(), "dataset:read", "project:" + dataset.getProjectId());

    if (!isAllowed) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to read this dataset");
    }

    return DatasetResponse.fromEntity(dataset);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteDataset(@PathVariable String id,
      @RequestHeader(value = "X-Forwarded-User", defaultValue = "system") String username) {

    Dataset dataset = datasetRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset not found"));

    boolean isAllowed = authPolicyClient.authorize(
        username, List.of(), "dataset:delete", "project:" + dataset.getProjectId());

    if (!isAllowed) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this dataset");
    }

    datasetManagerService.deleteDataset(id);
  }

  @GetMapping("/{id}/versions")
  public List<DatasetVersionResponse> getDatasetVersions(@PathVariable String id,
      @RequestHeader(value = "X-Forwarded-User", defaultValue = "system") String username) {

    Dataset dataset = datasetRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset not found"));

    boolean isAllowed = authPolicyClient.authorize(
        username, List.of(), "dataset:read", "project:" + dataset.getProjectId());

    if (!isAllowed) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to read this dataset");
    }

    return datasetVersionRepository.findByDatasetIdOrderByVersionNumberDesc(id)
        .stream()
        .map(DatasetVersionResponse::fromEntity)
        .collect(Collectors.toList());
  }

  // Example of a time-travel read endpoint returning just the schema for now
  // Actual data payloads would typically be too large for REST or returned via
  // pagination/streams.
  @GetMapping("/{id}/schema")
  public String getDatasetSchema(@PathVariable String id,
      @RequestParam(required = false) Long version,
      @RequestHeader(value = "X-Forwarded-User", defaultValue = "system") String username) {

    Dataset dataset = datasetRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset not found"));

    boolean isAllowed = authPolicyClient.authorize(
        username, List.of(), "dataset:read", "project:" + dataset.getProjectId());

    if (!isAllowed) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to read this dataset");
    }

    return deltaLakeService.getSchemaJson(dataset.getStoragePath());
  }
}
