package net.cbsolution.crypthru.crypt;

import net.cbsolution.crypthru.util.PathKit;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Default naming conventions are that encrypted files are named by suffixing the plain text fie name with ".pgp".
 */
public class DefaultNamingConvention implements NamingConvention {
  final static String SUFFIX = ".pgp";
  final static String SUFFIX2 = ".gpg";

  @Override
  public boolean isEncrypted(Path plainTextFileName) {
    String name = plainTextFileName.toString();
    return name.endsWith(SUFFIX) || name.endsWith(SUFFIX2);
  }

  @Override
  public Path encryptedName(Path plainTextFileName) {
    return PathKit.getParentPath(plainTextFileName).resolve(plainTextFileName.getFileName() + SUFFIX);
  }

  @Override
  public Path decryptedName(Path encryptedFileName) {
    if (!isEncrypted(encryptedFileName))
      return null;
    String newName = encryptedFileName.getFileName().toString().replaceFirst("\\.[^.]+$", "");
    return PathKit.getParentPath(encryptedFileName).resolve(newName);
  }


}
