package net.cbsolution.crypthru;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A generic directive to be executed.
 */
public interface Directive {

  /**
   * Configure directive parameters from a map.
   * @param config
   */
  void configure(ConfigurationDecoder config);

  /**
   * Print the directive.
   * @return a human-readable description of the directive.
   */
  String print();

  /**
   * Execute the directive
   * @param args program arguments
   */
  void execute(Arguments args);

  default void wipe(List<Path> files, boolean previewMode, InfoWriter infoWriter) {
    for (Path file : files) {
      String action = previewMode ? "Would delete " : "Deleting ";
      infoWriter.info(action + file);
      if (!previewMode) {
        try {
          Files.delete(file);
        } catch (IOException e) {
          throw new RuntimeException("Error deleting " + file, e);
        }
      }
    }
  }

  interface InfoWriter {
    void info(String text);
  }


}
