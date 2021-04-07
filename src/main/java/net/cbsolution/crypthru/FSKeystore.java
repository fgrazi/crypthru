package net.cbsolution.crypthru;

import lombok.extern.java.Log;
import net.cbsolution.crypthru.crypt.CrypterService;
import net.cbsolution.crypthru.crypt.KeyPairProxy;
import net.cbsolution.crypthru.crypt.KeyProxy;
import net.cbsolution.crypthru.crypt.PublicKeyProxy;
import net.cbsolution.crypthru.util.PathKit;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log
public class FSKeystore {

  // Expired files are in-fixed (after this prefix) with number of seconds since epoch
  private final static String EXPIRATION_PREFIX = "~";

  @Autowired
  private Arguments arguments;

  public static final String KEY = ".key";

  public Path getDirectory() {
    return getDirectory(null);
  }

  /**
   * @param toResolve A path inside the user configuration.
   * @return The associated directory, created if needed.
   */
  public Path getDirectory(String toResolve) {
    Path appHome = PathKit.replaceHome(arguments.getKeyStore());
    Path result = toResolve == null ? appHome : appHome.resolve(toResolve);
    return findOrMake(result);
  }

  private Path findOrMake(Path path) {
    if (!Files.exists(path)) {
      try {
        Files.createDirectories(path);
      } catch (IOException e) {
        throw new RuntimeException("Unable to create directory " + path, e);
      }
      log.info("Created directory: " + path);
    }
    return path;
  }

  /**
   * @param toResolve A path inside the home directory.
   * @return The associated file, upper directories created if needed.
   */
  public Path getHomeFile(String toResolve) {
    Path appHome = PathKit.replaceHome(arguments.getKeyStore());
    Path result = toResolve == null ? appHome : appHome.resolve(toResolve);
    findOrMake(PathKit.getParentPath(result));
    return result;
  }

  /**
   * @return The ID associated with file containing the private key (any file if more present), or null if
   * private key still missing.
   */
  public String pickPrivateKey() {
    try {
      Set<String> keyIds = Files.list(getDirectory("keys/private"))
          .filter(p -> !p.getFileName().toString().contains("~"))
          .map(p -> p.getFileName().toString().replaceFirst("\\" + KEY + "$", ""))
          .collect(Collectors.toSet());
      if (keyIds.isEmpty())
        throw new RuntimeException("There is no private key. Please execute the create-key-pair directive first.");
      if (keyIds.size() == 1)
        return keyIds.iterator().next();
      throw new RuntimeException("No -private-id specified and multiple private key found.");
    } catch (IOException e) {
      throw new RuntimeException("Unexpected exception", e);
    }
  }

  /**
   * @param id The ID of private key
   * @return The associated file.
   */
  public Path getPrivateKeyFile(String id) {
    Path result = getHomeFile("keys/private/" + id + KEY);
    if (!Files.exists(result))
      throw new RuntimeException("No private key for " + id + " (missing file " + result + ")");
    return result;
  }

  /**
   * @param id The ID of public key
   * @return The associated file.
   */
  public Path getPublicKeyFile(String id) {
    Path result = getHomeFile("keys/public/" + id + KEY);
    if (!Files.exists(result))
      throw new RuntimeException("No public key for " + id + " (file: " + result + ")");
    return result;
  }

  /**
   * Retrieve a set of public keys
   *
   * @param idOrPath either the ID of a public key or the name of a subdirectory containing all
   *                 public keys to be returned.
   * @return The associated set of files.
   */
  public Set<Path> getPublicKeyFiles(String idOrPath) {
    Path path = getHomeFile("keys/public/" + idOrPath);
    if (Files.exists(path) && Files.isDirectory(path)) {
      try {
        return Files.list(path).filter(p -> p.getFileName().toString().endsWith(KEY))
            .collect(Collectors.toSet());
      } catch (IOException e) {
        throw new RuntimeException("Unexpected exception scanning " + path, e);
      }
    }
    HashSet<Path> result = new HashSet();
    result.add(getPublicKeyFile(idOrPath));
    return result;
  }

  public static class KeyHistory implements Comparable<KeyHistory> {
    private final Path keyFile;
    private final Date expiration;

    public KeyHistory(Path keyFile, Date expiration) {
      this.keyFile = keyFile;
      this.expiration = expiration;
    }

    @Override
    public int compareTo(KeyHistory o) {
      // most recent comes first
      if (expiration == null)
        return -1;
      else if (o.expiration == null)
        return 1;
      return -expiration.compareTo(o.expiration);
    }

    private Date getCreation() {
      try {
        return new Date(Files.getLastModifiedTime(keyFile).toMillis());
      } catch (IOException e) {
        throw new RuntimeException("Unexpected exception getting time of " + keyFile, e);
      }
    }

    @Override
    public String toString() {
      return "KeyHistory{" + keyFile + " " + getCreation() + " ... " +
          (expiration == null ? "" : expiration) + "}";
    }
  }

  private static void rename(Path oldName, Path newName) {
    try {
      Files.move(oldName, newName);
    } catch (IOException ex) {
      throw new RuntimeException("Error reaming " + oldName + " to " + newName);
    }
  }

  /**
   * Create a new file for a given ID, renaming the current file if already present
   *
   * @param parent The parent directory.
   * @param id     The ID of the key.
   * @param key    The key to be written.
   * @return the created file.
   */
  public static Path saveKey(Path parent, String id, KeyProxy key) {
    try {
      Path newFile = parent.resolve(id + KEY);
      Path tempFile = null;
      if (Files.exists(newFile))
        rename(newFile, tempFile = Paths.get(newFile.toString() + ".bak"));
      key.writeTo(newFile);
      if (tempFile != null) {
        long expiration = Files.getLastModifiedTime(newFile).toMillis() / 1000;
        String expiredFileName = newFile.toString().replaceFirst("\\.([^.]+)$",
            EXPIRATION_PREFIX + expiration + ".$1");
        Path renamedFile = Paths.get(expiredFileName);
        rename(tempFile, renamedFile);
      }
      return newFile;
    } catch (IOException ex) {
      throw new RuntimeException("Error creating Key File for " + id + " under " + parent, ex);
    }
  }

  public static List<KeyHistory> getHistory(Path parent, String id) {
    Pattern expiredPattern = Pattern.compile("^" + id + EXPIRATION_PREFIX + "([0-9]+)\\" + KEY);
    List<KeyHistory> result = new ArrayList<>();
    try {
      Iterator<Path> iterator = Files.newDirectoryStream(parent, id + "*.*").iterator();
      while (iterator.hasNext()) {
        Path p = iterator.next();
        String fileName = p.getFileName().toString();
        Matcher m = expiredPattern.matcher(fileName);
        if (m.matches()) {
          Date expiration = new Date(1000L * Integer.parseInt(m.group(1)));
          result.add(new KeyHistory(p, expiration));
        } else if ((id + KEY).equals(fileName)) {
          result.add(new KeyHistory(p, null));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Unexpected exception", e);
    }
    Collections.sort(result);
    return result;
  }

  /**
   * Search a key at a valid date
   *
   * @param parent    The parent directory
   * @param id        The ID of the key
   * @param validDate The date at which the key shall be valid.
   */
  public static Path lookupKey(Path parent, String id, Date validDate) {
    Path current = parent.resolve(id + KEY);

    // use current file if possible
    try {
      Date currentKeyDate = new Date(Files.getLastModifiedTime(current).toMillis());
      if (Files.exists(current) && currentKeyDate.compareTo(validDate) < 0)
        return current;
    } catch (IOException e) {
      throw new RuntimeException("Unexpected exception reading date of " + current, e);
    }
    List<KeyHistory> history = FSKeystore.getHistory(parent, id);
    for (KeyHistory h : history) {
      if (h.expiration != null && validDate.compareTo(h.expiration) <= 0 &&
          validDate.compareTo(h.getCreation()) >= 0)
        return h.keyFile;
    }
    return null;
  }

  public Path[] save(String id, KeyPairProxy keyPair) {
    Path privateFile = saveKey(getDirectory("keys/private"), id, keyPair.getPrivateKeyProxy());
    Path publicFile = saveKey(getDirectory("keys/public"), id, keyPair.getPublicKeyProxy());
    return new Path[]{privateFile, publicFile};
  }

  public Path save(String id, PublicKeyProxy publicKey) {
    return saveKey(getDirectory("home/public"), id, publicKey);
  }

}
