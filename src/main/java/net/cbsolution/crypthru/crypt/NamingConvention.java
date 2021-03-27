package net.cbsolution.crypthru.crypt;

import java.nio.file.Path;

/**
 * Determine how plain text file path name shall be obtained from encrypted
 * file path and the other way around.
 */
public interface NamingConvention {

  /**
   * @param plainTextFileName
   * @return true if the path is of an encrypted file.
   */
  boolean isEncrypted(Path plainTextFileName);

  /**
   * Assign the name to the encrypted file.
   * @param plainTextFileName Name of a plain text file
   * @return Name of resulting encrypted file.
   */
  Path encryptedName(Path plainTextFileName);

  /**
   * Assign the name to a decrypted file,
   * @param encryptedFileName
   * @return Either null if encryptedFileName is not a name of an encrypted file
   * or the name of the original plain text file.
   */
  Path decryptedName(Path encryptedFileName);

}
