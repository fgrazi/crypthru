package net.cbsolution.crypthru.crypt;

import lombok.extern.java.Log;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.*;


/**
 * After https://qastack.it/unix/60213/gpg-asks-for-password-even-with-passphrase
 *
 * gpg --pinentry-mode loopback --passphrase-file=file encrypted.gpg
 *
 * to clean console:
 *
 *     gpg-connect-agent reloadagent /bye
 *
 */

@Log
/**
 * Executes GPG in background
 */
public class GPGWrapper {

  public static void runDecrypt(Path encryptedFile, Path plainTextFile, String privateKeyId, String passphrase) {
    List<String> args = new ArrayList<>();
    if (!passphrase.isEmpty() && !"empty".equals(passphrase)) {
      args.add("--pinentry-mode");
      args.add("loopback");
      args.add("--passphrase=" + passphrase);
    }
    args.add("-r");
    args.add(privateKeyId);
    args.add("--output");
    args.add(plainTextFile.toString());
    args.add("--decrypt");
    args.add(encryptedFile.toString());
    runGpgCommand(args);
  }

  public static void runEncrypt(Path plainTextFile, Path encryptedFile, Collection<String> recipientIds) {
    List<String> args = new ArrayList<>();
    for (String id : recipientIds) {
      args.add("-r");
      args.add(id);
    }
    args.add("--encrypt");
    args.add(plainTextFile.toString());
    runGpgCommand(args);
  }

  public static void runGpgCommand(List<String> arguments) {
    List<String> fullCommand = new ArrayList<>();
    fullCommand.add(System.getProperty("GPG", "gpg"));
    fullCommand.add("--yes");
    fullCommand.add("--batch");
    fullCommand.addAll(arguments);

    String[] tokens = fullCommand.toArray(new String[arguments.size()]);
    log.info("Executing GPG: " + String.join(" ", tokens));

    final ProcessBuilder builder = new ProcessBuilder(tokens).redirectErrorStream(true);
    try {
      final Process process = builder.start();
      final StringWriter writer = new StringWriter();

      new Thread(new Runnable() {
        public void run() {
          try {
            IOUtils.copy(process.getInputStream(), writer, "UTF-8");
          } catch (IOException ex) {
            throw new RuntimeException("Error executing: " + String.join(" ", tokens), ex);
          }
        }
      }).start();

      OutputStream stdin = process.getOutputStream();

      final int exitValue = process.waitFor();
      final String processOutput = writer.toString();
      log.info("GPG console:\n" + processOutput);
      if (exitValue != 0) {
        throw new RuntimeException("Exit code " + exitValue + " when running " + String.join(" ", tokens));
      }
    } catch (IOException | InterruptedException ex) {
      throw new RuntimeException("Error executing: " + String.join(" ", tokens), ex);
    }
  }

}
