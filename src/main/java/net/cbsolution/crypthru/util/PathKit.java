package net.cbsolution.crypthru.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.List;
import java.util.stream.Collectors;

public class PathKit {

  /**
   * Find first mismatch character between two files
   *
   * @param file1 First file to compare
   * @param file2 Second file to compare
   * @return Offset (from zero) of first unmatched character ot -1L if content of the files is the same.
   */
  public static long findMismatch(Path file1, Path file2) {
    try {
      BufferedInputStream fis1 = new BufferedInputStream(Files.newInputStream(file1));
      BufferedInputStream fis2 = new BufferedInputStream(Files.newInputStream(file2));
      long pos = -1;
      int b1 = 0, b2 = 0;
      while (b1 != -1 && b2 != -1) {
        pos++;
        b1 = fis1.read();
        b2 = fis2.read();
        if (b1 != b2)
          return pos;
      }
      fis1.close();
      fis2.close();
    } catch (IOException ex) {
      throw new RuntimeException("Error comparing " + file1 + " with " + file2, ex);
    }
    return -1L;
  }

  public static Path replaceHome(String path) {
    if (!path.startsWith("~"))
      return Paths.get(path);
    return Paths.get(path.replaceFirst("^~", System.getProperty("user.home").replaceAll("\\\\", "/")));
  }

  public static void dump(URL url, OutputStream target) {
    try {
      InputStream source = url.openStream();
      byte[] buf = new byte[8192];
      int length;
      while ((length = source.read(buf)) > 0) {
        target.write(buf, 0, length);
      }
      source.close();
    } catch (IOException ex) {
      throw new RuntimeException("IO error dumping " + url, ex);
    }
  }

  public static boolean isOutdated(Path source, Path result) {
    if (!Files.exists(result))
      return true;
    try {
      if (Files.getLastModifiedTime(result).compareTo(Files.getLastModifiedTime(source)) < 0)
        return true;
    } catch (IOException e) {
      throw new RuntimeException("Unexpected exception", e);
    }
    return false;
  }

  public static Path getParentPath(Path path) {
    Path result = path.getParent();
    if (result == null) // relative
      result = FileSystems.getDefault().getPath(path.toString()).normalize().toAbsolutePath().getParent();
    return result;
  }

}
