package net.cbsolution.crypthru.crypt;

import java.nio.file.Path;

/**
 * A generic Encrypt / Decrypt service
 */
public interface CrypterService {

  /**
   * @param keyFile a file.
   * @return the contained public key.
   */
  PublicKeyProxy readPublicKey(Path keyFile);

  /**
   * @param keyFile a file.
   * @return the contained private key.
   */
  PrivateKeyProxy readPrivateKey(Path keyFile, String passPhrase);

  void encrypt(Path plainTextFile, Path encryptedFile, PublicKeyProxy... keys);

  void decrypt(Path encryptedFile, Path plainTextFile, PrivateKeyProxy key);

  /**
   * Generate a private-public key pair.
   * @param identity An ID to identify the key (usually an email, phone number and so on)
   * @param passphrase A secret pass-phrase to access the private key.
   * @return The generated key-pair.
   */
  KeyPairProxy createKeyPair(String identity, String passphrase);

}
