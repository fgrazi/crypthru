package net.cbsolution.crypthru.crypt;

import net.cbsolution.crypthru.util.PathKit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = BcPgpService.class)
class BcPgpServiceTest {

  private Path tmpDir;

  private final String PASSPHRASE = "lalala";
  private final String PLAIN_TEXT = "The quick brown fox jumps over the lazy dog";

  @Autowired
  private CrypterService service;

  @Test
  public void fullCycleTest() throws Exception {

    tmpDir = Files.createTempDirectory("tmpDirPrefix");

    // Create a new keypair
    KeyPairProxy kp = service.createKeyPair("myId@myCo.com", PASSPHRASE);

    // Save the keypair to files
    kp.getPrivateKeyProxy().writeTo(path("private.gen.pgp"));
    kp.getPublicKeyProxy().writeTo(path("public.gen.pgp"));

    // Restore the keypair from file system
    kp = new KeyPairProxy(
        service.readPrivateKey(path("private.gen.pgp"), "lalala"),
        service.readPublicKey(path("public.gen.pgp")));

    // Create a second crypt key
    KeyPairProxy kp2 = service.createKeyPair("mySecondId@myCo.com", "Fodase!");

    // Create a plain text file
    Path plainTextFile = Paths.get(tmpDir.toString(), "plainTextFile.txt");
    Path encryptedFile = Paths.get(plainTextFile + ".pgp");
    Path decryptedFile = Paths.get(plainTextFile + ".restored");
    Path decryptedFile2 = Paths.get(plainTextFile + ".restored2");

    Files.write(plainTextFile, PLAIN_TEXT.getBytes(StandardCharsets.UTF_8));

    // Encrypt the file
    service.encrypt(plainTextFile, encryptedFile, kp.publicKeyProxy, kp2.publicKeyProxy);

    // Decrypt the file
    service.decrypt(encryptedFile, decryptedFile, kp.privateKeyProxy);

    // Ensure no mismatch
    assertEquals(-1L, PathKit.findMismatch(plainTextFile, decryptedFile));

    // Do the same with second key
    service.decrypt(encryptedFile, decryptedFile2, kp2.privateKeyProxy);
    assertEquals(-1L, PathKit.findMismatch(plainTextFile, decryptedFile2));

  }

  private Path path(String fileName) {
    return Paths.get(tmpDir.toString(), fileName);
  }

}