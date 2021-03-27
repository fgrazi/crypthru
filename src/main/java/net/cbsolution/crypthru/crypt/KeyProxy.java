package net.cbsolution.crypthru.crypt;

import java.nio.file.Path;

/**
 * A generic reference to a cryptographic key. Implementations usually wrap a specific key.
 */
public interface KeyProxy {

  /**
   * Saves the key in a file.
   * @param file the file that will contain the key.
   */
  void writeTo(Path file);


  /**
   * @return Readable information about the key.
   */
  String print();

}
