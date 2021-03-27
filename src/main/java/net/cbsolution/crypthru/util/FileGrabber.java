package net.cbsolution.crypthru.util;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A generic grabber of files in a directory.
 */
public class FileGrabber {
  private final String fileOrDirectory;
  private final Path directory;
  private final Path singleFile;
  private final List<GlobFilter> patternFilters = new ArrayList<>();
  boolean acceptByDefault;

  public FileGrabber(String fileOrDirectory) {
    Path fullPath = PathKit.replaceHome(this.fileOrDirectory = fileOrDirectory);
    if (Files.isDirectory(fullPath)) {
      directory = fullPath;
      singleFile = null;
      acceptByDefault = true;
    } else {
      String fileName = fullPath.getFileName().toString();
      if (fileName.contains("?") || fileName.contains("{") || fileName.contains("*")) {
        directory = fullPath.getParent();
        singleFile = null;
        filter(GlobFilter.Action.INCLUDE, fileName);
      } else {
        directory = null;
        singleFile = fullPath;
      }
    }
  }

  public Path getDirectory() {
    return directory;
  }

  private boolean passPatterns(Path file) {
    if (patternFilters.isEmpty())
      return true; // no filter to apply
    Path fileName = file.getFileName();
    for (GlobFilter f: patternFilters) {
      if (f.pathMatcher.matches(fileName)) {
        switch (f.action) {
          case EXCLUDE:
            return false;
          case INCLUDE:
            return true;
        }
      }
    }
    return acceptByDefault;
  }

  public void filter(GlobFilter.Action action, String pattern) {
    patternFilters.add(new GlobFilter(action, pattern));
  }

  public List<Path> grab(Filter filter) {
    if (singleFile != null) {
      if (!Files.exists(singleFile))
        throw new RuntimeException("No such file: " + singleFile);
      List<Path> result = new ArrayList<>();
      if (passPatterns(singleFile))
        result.add(singleFile);
      return result;
    }
    if (!Files.isDirectory(directory))
      throw new RuntimeException("No such directory: " + directory);
    try {
      return Files.list(directory)
          .filter(p -> filter == null ? true : filter.accept(p))
          .filter(p -> passPatterns(p))
          .collect(Collectors.toList());
    } catch (IOException ex) {
      throw new RuntimeException("IO Error listing the content of " + directory, ex);
    }
  }

  public static class GlobFilter {
    public enum Action { INCLUDE, EXCLUDE }

    private final Action action;
    private final PathMatcher pathMatcher;
    private final String pattern;

    public GlobFilter(Action action, String pattern) {
      this.action = action;
      this.pathMatcher = FileSystems.getDefault().getPathMatcher(
          "glob:" + (this.pattern =  pattern));
    }

    public String print() {
      return (action == Action.INCLUDE ? "+" : "-") + pattern;
    }

  }

  public interface Filter {
    boolean accept(Path path);
  }

  public String print() {
    String strFilter = patternFilters.stream().map(GlobFilter::print).collect(Collectors.joining(", "));
    return strFilter.isEmpty() ? fileOrDirectory : (fileOrDirectory + " (" + strFilter + ")");
  }

}
