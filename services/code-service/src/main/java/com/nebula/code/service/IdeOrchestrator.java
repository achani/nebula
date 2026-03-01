package com.nebula.code.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.nebula.code.domain.IdeSession;
import com.nebula.code.domain.Repository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.ServerSocket;
import java.time.Duration;

@Slf4j
@Service
public class IdeOrchestrator {

  private final DockerClient dockerClient;
  private final String dockerNetwork;
  private final String proxyDomain;
  private final String hostGitStoragePath;

  public IdeOrchestrator(
      @Value("${nebula.ide.docker-host}") String dockerHost,
      @Value("${nebula.ide.network-name}") String dockerNetwork,
      @Value("${nebula.ide.proxy-domain}") String proxyDomain,
      @Value("${HOST_GIT_STORAGE_PATH:/var/nebula/git}") String hostGitStoragePath) {

    this.dockerNetwork = dockerNetwork;
    this.proxyDomain = proxyDomain;
    this.hostGitStoragePath = hostGitStoragePath;

    DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerHost(dockerHost)
        .build();

    DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
        .dockerHost(config.getDockerHost())
        .maxConnections(100)
        .connectionTimeout(Duration.ofSeconds(30))
        .responseTimeout(Duration.ofSeconds(45))
        .build();

    this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
  }

  public IdeSession provisionIde(Repository repository, File repoWorkspaceDir) {
    int port = findFreePort();
    // Base paths inside container for code-server
    String containerWorkspacePath = "/config/workspace";

    // Translate container-internal path to host path
    // container path: /var/nebula/git/{id}-workspace
    // host path: {hostGitStoragePath}/{id}-workspace
    String hostWorkspacePath = hostGitStoragePath + "/" + repoWorkspaceDir.getName();

    HostConfig hostConfig = HostConfig.newHostConfig()
        .withBinds(new Bind(hostWorkspacePath, new Volume(containerWorkspacePath)))
        .withPortBindings(new PortBinding(Ports.Binding.bindPort(port), ExposedPort.tcp(8443)))
        .withNetworkMode(dockerNetwork);

    CreateContainerResponse container = dockerClient.createContainerCmd("linuxserver/code-server:latest")
        .withName("ide-" + repository.getId().toString())
        .withHostConfig(hostConfig)
        .withEnv("PUID=1000", "PGID=1000", "TZ=Etc/UTC", "PASSWORD=nebula", "SUDO_PASSWORD=nebula",
            "GIT_SAFE_DIRECTORIES=/config/workspace")
        .withExposedPorts(ExposedPort.tcp(8443))
        .exec();

    String containerId = container.getId();
    if (containerId == null) {
      throw new RuntimeException("Failed to create IDE container: ID is null");
    }
    dockerClient.startContainerCmd(containerId).exec();

    log.info("Started code-server container {} for repository {} on port {}",
        containerId, repository.getId(), port);

    return IdeSession.builder()
        .repository(repository)
        .containerId(containerId)
        .port(port)
        .status(IdeSession.IdeSessionStatus.RUNNING)
        .build();
  }

  public void stopIde(IdeSession session) {
    String containerId = session.getContainerId();
    if (containerId == null) {
      log.warn("Cannot stop IDE session: containerId is null");
      return;
    }
    try {
      dockerClient.stopContainerCmd(containerId).withTimeout(10).exec();
      dockerClient.removeContainerCmd(containerId).withForce(true).exec();
      log.info("Stopped and removed IDE container {}", containerId);
    } catch (Exception e) {
      log.error("Failed to stop container {}", containerId, e);
    }
  }

  public String getIdeProxyUrl(IdeSession session) {
    // We will configure API gateway to map /ide/{sessionId} to code-service:port
    // OR return the direct docker mapped port for local dev
    return "http://localhost:" + session.getPort();
  }

  private int findFreePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      return socket.getLocalPort();
    } catch (Exception e) {
      throw new RuntimeException("Could not find a free port for IDE", e);
    }
  }
}
