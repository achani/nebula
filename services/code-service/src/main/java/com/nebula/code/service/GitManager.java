package com.nebula.code.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GitManager {

  private final String gitStoragePath;

  public GitManager(@Value("${nebula.git.storage-path}") String gitStoragePath) {
    this.gitStoragePath = gitStoragePath;
  }

  public File getRepoDirectory(UUID repositoryId) {
    return new File(gitStoragePath, repositoryId.toString() + ".git");
  }

  public File getWorkspaceDirectory(UUID repositoryId) {
    return new File(gitStoragePath, repositoryId.toString() + "-workspace");
  }

  public void initializeBareRepository(UUID repositoryId) {
    File repoDir = getRepoDirectory(repositoryId);
    try {
      Git.init().setDirectory(repoDir).setBare(true).call().close();
    } catch (GitAPIException | IllegalStateException e) {
      throw new RuntimeException("Failed to initialize bare repository " + repositoryId, e);
    }
  }

  public void ensureWorkspace(UUID repositoryId) {
    File repoDir = getRepoDirectory(repositoryId);
    File workspaceDir = getWorkspaceDirectory(repositoryId);

    if (!workspaceDir.exists()) {
      try {
        // Try to clone. If repo is empty, this might fail or create an empty clone.
        Git.cloneRepository()
            .setURI(repoDir.getAbsolutePath())
            .setDirectory(workspaceDir)
            .call()
            .close();
      } catch (GitAPIException e) {
        // If it fails (e.g. empty repo), just create the directory
        if (!workspaceDir.exists()) {
          workspaceDir.mkdirs();
        }
        // Initialize as a normal repo and set origin
        try {
          Git git = Git.init().setDirectory(workspaceDir).call();
          git.getRepository().getConfig().setString("remote", "origin", "url", repoDir.getAbsolutePath());
          git.getRepository().getConfig().save();
          git.close();
        } catch (Exception ex) {
          throw new RuntimeException("Failed to initialize workspace for empty repo", ex);
        }
      }
    }
  }

  public List<String> listFiles(UUID repositoryId, String branchPath) {
    File repoDir = getRepoDirectory(repositoryId);
    List<String> files = new ArrayList<>();

    if (!repoDir.exists()) {
      return files;
    }

    try (org.eclipse.jgit.lib.Repository repo = Git.open(repoDir).getRepository();
        RevWalk revWalk = new RevWalk(repo)) {

      ObjectId headId = repo.resolve(branchPath);
      if (headId == null) {
        return files; // Branch doesn't exist yet (empty repo)
      }

      RevCommit commit = revWalk.parseCommit(headId);
      RevTree tree = commit.getTree();

      try (TreeWalk treeWalk = new TreeWalk(repo)) {
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        while (treeWalk.next()) {
          files.add(treeWalk.getPathString());
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to list files for " + repositoryId, e);
    }

    return files;
  }

  public String readFileContent(UUID repositoryId, String branchPath, String filePath) {
    File repoDir = getRepoDirectory(repositoryId);
    try (org.eclipse.jgit.lib.Repository repo = Git.open(repoDir).getRepository();
        RevWalk revWalk = new RevWalk(repo)) {

      ObjectId headId = repo.resolve(branchPath);
      if (headId == null)
        return null;

      RevCommit commit = revWalk.parseCommit(headId);
      RevTree tree = commit.getTree();

      try (TreeWalk treeWalk = new TreeWalk(repo)) {
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        treeWalk.setFilter(PathFilter.create(filePath));

        if (!treeWalk.next()) {
          return null; // File not found
        }

        ObjectId objectId = treeWalk.getObjectId(0);
        ObjectLoader loader = repo.open(objectId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        loader.copyTo(out);
        return out.toString();
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file content for " + filePath, e);
    }
  }
}
