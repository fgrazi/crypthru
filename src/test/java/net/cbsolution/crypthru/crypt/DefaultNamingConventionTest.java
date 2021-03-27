package net.cbsolution.crypthru.crypt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DefaultNamingConvention.class)
public class DefaultNamingConventionTest {

  @Autowired
  DefaultNamingConvention convention;

  @Test
  void encryptedName() {
    Path file = Paths.get("/a/b/c.txt");
    assertEquals("/a/b/c.txt.pgp", convention.encryptedName(file).toString());
  }

  @Test
  void decryptedName() {
    Path file = Paths.get("/a/b/c.txt.pgp");
    assertEquals("/a/b/c.txt", convention.decryptedName(file).toString());
    file = Paths.get("/a/b/c.txt.gpg");
    assertEquals("/a/b/c.txt", convention.decryptedName(file).toString());
  }

  @Test
  void decryptedIllegalName() {
    Path file = Paths.get("/a/b/c.txt");
    assertNull(convention.decryptedName(file));
  }

}