package com.nebula.catalog.service.grpc;

import com.nebula.catalog.domain.Folder;
import com.nebula.catalog.domain.Project;
import com.nebula.catalog.grpc.*;
import com.nebula.catalog.repository.FolderRepository;
import com.nebula.catalog.repository.ProjectRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class CatalogLookupServiceImpl extends CatalogLookupServiceGrpc.CatalogLookupServiceImplBase {

  private final ProjectRepository projectRepository;
  private final FolderRepository folderRepository;

  @Override
  @Transactional(readOnly = true)
  public void lookupProject(LookupProjectRequest request, StreamObserver<LookupProjectResponse> responseObserver) {
    try {
      UUID id = UUID.fromString(request.getProjectId());
      Project project = projectRepository.findById(id)
          .orElseThrow(() -> Status.NOT_FOUND.withDescription("Project not found").asRuntimeException());

      LookupProjectResponse response = LookupProjectResponse.newBuilder()
          .setId(project.getId().toString())
          .setName(project.getName())
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (IllegalArgumentException e) {
      responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid UUID format").asRuntimeException());
    } catch (RuntimeException e) {
      // Pass through StatusRuntimeException directly
      responseObserver.onError(e);
    } catch (Exception e) {
      responseObserver
          .onError(Status.INTERNAL.withCause(e).withDescription("Internal server error").asRuntimeException());
    }
  }

  @Override
  @Transactional(readOnly = true)
  public void lookupFolder(LookupFolderRequest request, StreamObserver<LookupFolderResponse> responseObserver) {
    try {
      UUID id = UUID.fromString(request.getFolderId());
      Folder folder = folderRepository.findById(id)
          .orElseThrow(() -> Status.NOT_FOUND.withDescription("Folder not found").asRuntimeException());

      LookupFolderResponse response = LookupFolderResponse.newBuilder()
          .setId(folder.getId().toString())
          .setProjectId(folder.getProject().getId().toString())
          .setName(folder.getName())
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (IllegalArgumentException e) {
      responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid UUID format").asRuntimeException());
    } catch (RuntimeException e) {
      responseObserver.onError(e);
    } catch (Exception e) {
      responseObserver
          .onError(Status.INTERNAL.withCause(e).withDescription("Internal server error").asRuntimeException());
    }
  }
}
