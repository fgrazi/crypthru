package net.cbsolution.crypthru.directives;

import lombok.extern.java.Log;
import net.cbsolution.crypthru.Arguments;
import net.cbsolution.crypthru.ConfigurationDecoder;
import net.cbsolution.crypthru.Directive;
import net.cbsolution.crypthru.crypt.DefaultNamingConvention;
import net.cbsolution.crypthru.crypt.GPGWrapper;
import net.cbsolution.crypthru.crypt.NamingConvention;
import net.cbsolution.crypthru.util.DirectoryWatcher;
import net.cbsolution.crypthru.util.FileGrabber;
import net.cbsolution.crypthru.util.PathKit;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Log
public class Decrypt implements Directive {
  private NamingConvention namingConvention = new DefaultNamingConvention();
  private FileGrabber fileGrabber;
  private boolean wipe;
  private boolean unzip;
  private boolean runGpg;

  @Override
  public void configure(ConfigurationDecoder config) {
    wipe = config.read("wipe", false);
    fileGrabber = new FileGrabber(config.readString("path"));
    unzip = config.read("unzip", false);
    config.captureFilters(fileGrabber);
    runGpg = config.read("gpg", false);
  }

  @Override
  public String print() {
    return MessageFormat.format("Decrypt {0}", fileGrabber.print());
  }

  private boolean needsDecrypting(Path p, boolean force) {
    if (!namingConvention.isEncrypted(p))
      return false;
    if (force)
      return true;
    Path decrypted = namingConvention.decryptedName(p);
    return PathKit.isOutdated(p, decrypted);
  }

  @Override
  public void execute(Arguments args) {
    List<Path> files = fileGrabber.grab(p -> needsDecrypting(p, args.isForce()));
    decrypt(files, args);
    wipeIfApplicable(files, args);
    if (args.isWatch()) {
      log.info("Watching directory " + fileGrabber.getDirectory() + " to decrypt new files. Drop a file named \"STOP\" to terminate.");
      DirectoryWatcher watcher = new DirectoryWatcher(fileGrabber.getDirectory());
      watcher.stopOn("STOP").filter(p -> needsDecrypting(p, args.isForce()))
          .react((e, p) -> {
            decrypt(p, args);
            wipeIfApplicable(Arrays.asList(new Path[]{p}), args);
          });
    }
  }

  private void wipeIfApplicable(List<Path> files, Arguments args) {
    if (wipe)
      wipe(files, args.isPreviewMode(), text -> {
        log.info(text);
      });
  }

  void decrypt(List<Path> files, Arguments args) {
    for (Path encryptedFile : files) {
      decrypt(encryptedFile, args);
    }
  }

  void decrypt(Path encryptedFile, Arguments args) {
    Path decryptedFile = namingConvention.decryptedName(encryptedFile);
    String action = args.isPreviewMode() ? "Would decrypt " : "Decrypting ";
    log.info(action + encryptedFile + " into " + namingConvention.decryptedName(encryptedFile));
    if (!args.isPreviewMode()) {
      Date start = new Date();
      if (runGpg || args.isRunGpg())
        GPGWrapper.runDecrypt(encryptedFile, decryptedFile, args.getPrivateKeyId(),
            args.getOrAskPassphrase(args.getPrivateKeyId()));
      else {
        args.getCryptService().decrypt(encryptedFile, decryptedFile, args.figurePrivateKey());
      }
      Date end = new Date();
      args.getDecryptPerformance().record(start, end, decryptedFile);
    }
    if (decryptedFile.getFileName().toString().endsWith(".zip") && unzip) {
      action = args.isPreviewMode() ? "Would unzip " : "Unzipping ";
      log.info(action + decryptedFile);
      if (!(args.isPreviewMode()))
        doUnzip(decryptedFile);
    }
  }


  void doUnzip(Path zipFile) {
    byte[] buffer = new byte[1024];
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toString()))) {
      ZipEntry zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
        Path newFile = newFile(PathKit.getParentPath(zipFile), zipEntry);
        if (zipEntry.isDirectory()) {
          if (!Files.isDirectory(newFile) && !newFile.toFile().mkdirs()) {
            throw new IOException("Failed to create directory " + newFile);
          }
        } else {

          // fix for Windows-created archives
          Path parent = PathKit.getParentPath(newFile);
          if (!Files.isDirectory(parent) && !parent.toFile().mkdirs()) {
            throw new IOException("Failed to create directory " + parent);
          }

          // write file content
          try (FileOutputStream fos = new FileOutputStream(newFile.toString())) {
            int len;
            while ((len = zis.read(buffer)) > 0) {
              fos.write(buffer, 0, len);
            }
          } catch (IOException ex) {
            throw new RemoteException("I/O Error extracting " + newFile + " from " + zipFile, ex);
          }
        }
        zipEntry = zis.getNextEntry();
      }
      zis.closeEntry();
      Files.delete(zipFile);
    } catch (IOException ex) {
      throw new RuntimeException("I/O Error extracting from " + zipFile, ex);
    }
  }

  public static Path newFile(Path destinationDir, ZipEntry zipEntry) throws IOException {
    Path destFile = destinationDir.resolve(zipEntry.getName());
    String destDirPath = destinationDir.toFile().getCanonicalPath();
    String destFilePath = destFile.toFile().getCanonicalPath();
    if (!destFilePath.startsWith(destDirPath + FileSystems.getDefault().getSeparator())) {
      throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
    }
    return destFile;
  }

}
