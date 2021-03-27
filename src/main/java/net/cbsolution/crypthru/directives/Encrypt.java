package net.cbsolution.crypthru.directives;

import lombok.extern.java.Log;
import net.cbsolution.crypthru.Arguments;
import net.cbsolution.crypthru.ConfigurationDecoder;
import net.cbsolution.crypthru.Directive;
import net.cbsolution.crypthru.FSKeystore;
import net.cbsolution.crypthru.crypt.*;
import net.cbsolution.crypthru.util.DirectoryWatcher;
import net.cbsolution.crypthru.util.FileGrabber;
import net.cbsolution.crypthru.util.PathKit;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log
public class Encrypt implements Directive {
  private boolean wipe;
  private String zip;
  private NamingConvention namingConvention = new DefaultNamingConvention();
  private PublicKeyCollector collector = new PublicKeyCollector();
  private FileGrabber fileGrabber;
  private boolean runGpg;

  @Override
  public void configure(ConfigurationDecoder config) {
    wipe = config.read("wipe", false);
    zip = config.readString("zip", "");
    fileGrabber = new FileGrabber(config.readString("path"));
    config.captureFilters(fileGrabber);
    collector.addKeyIds(Arrays.asList(config.readStrings("public-id")));
    collector.addKeyFiles(Arrays.asList(config.readStrings("public-key")));
    runGpg = config.read("gpg", false);
  }

  @Override
  public String print() {
    return MessageFormat.format("Encrypt {0}", fileGrabber.print());
  }

  private boolean needsEncrypting(Path p, boolean force) {
    if (namingConvention.isEncrypted(p))
      return false;
    if (force)
      return true;
    Path encrypted = namingConvention.encryptedName(p);
    return PathKit.isOutdated(p, encrypted);
  }

  @Override
  public void execute(Arguments args) {
    List<Path> files = fileGrabber.grab(p -> needsEncrypting(p, args.isForce()) || !zip.isEmpty());
    if (!files.isEmpty()) {
      collector.addKeyIds(args.getPublicKeys());
      collector.addKeyFiles(args.getPublicKeyFiles());
      collector.collect(args.getFsKeystore(), args.getCryptService(), runGpg || args.isRunGpg());
      if (!zip.isEmpty()) {
        Path zipFile = buildZip(files, zip, args.isPreviewMode());
        List<Path> lZip = Arrays.asList(new Path[]{zipFile});
        encrypt(lZip, args);
        wipe(lZip, args.isPreviewMode(), text -> {
          log.info(text);
        });
      } else
        encrypt(files, args);
      wipeIfApplicable(files, args);
    }
    if (args.isWatch()) {
      log.info("Watching directory " + fileGrabber.getDirectory() + " to encrypt new files. Drop a file named \"STOP\" to terminate.");
      DirectoryWatcher watcher = new DirectoryWatcher(fileGrabber.getDirectory());
      watcher.stopOn("STOP").filter(p -> needsEncrypting(p, args.isForce()))
          .react((e, p) -> {
            encrypt(p, args);
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

  private void encrypt(List<Path> files, Arguments args) {
    for (Path file : files) {
      encrypt(file, args);
    }
  }

  private void encrypt(Path file, Arguments args) {
    Path encryptedFile = namingConvention.encryptedName(file);
    String action = args.isPreviewMode() ? "Would encrypt " : "Encrypting ";
    log.info(action + file + " into " + namingConvention.encryptedName(file) + " for " +
        collector.ids);
    if (!args.isPreviewMode()) {
      Date start = new Date();
      if (runGpg || args.isRunGpg()) {
        GPGWrapper.runEncrypt(file, encryptedFile, collector.publicKeyIds);
      } else
        args.getCryptService().encrypt(file, encryptedFile, collector.keys);
      Date end = new Date();
      args.getEncryptPerformance().record(start, end, file);
    }
  }


  private Path buildZip(List<Path> files, String zip, boolean previewMode) {
    if (!zip.endsWith(".zip"))
      zip += ".zip";
    Path zipPath = fileGrabber.getDirectory().resolve(zip);
    if (previewMode) {
      for (Path input : files) {
        log.info("Would zip " + input + " into " + zipPath);
      }
    } else {
      try (FileOutputStream fos = new FileOutputStream(zipPath.toString())) {
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        for (Path input : files) {
          log.info("Zipping " + input + " into " + zipPath);
          try (InputStream fis = new FileInputStream(input.toString())) {
            ZipEntry zipEntry = new ZipEntry(input.getFileName().toString());
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
              zipOut.write(bytes, 0, length);
            }
          } catch (IOException ex) {
            throw new RuntimeException("I/O error zipping " + input + " into " + zipPath, ex);
          }
        }
        zipOut.close();
      } catch (IOException ex) {
        throw new RuntimeException("I/O Error zipping into " + zipPath, ex);
      }
    }
    return zipPath;
  }

  private static class PublicKeyCollector {
    Set<String> publicKeyFiles = new HashSet<>();
    Set<String> publicKeyIds = new HashSet<>();
    String ids;
    PublicKeyProxy[] keys;

    void addKeyFiles(List<String> keyFiles) {
      publicKeyFiles.addAll(keyFiles);
    }

    void addKeyIds(List<String> keyIds) {
      publicKeyIds.addAll(keyIds);
    }

    void collect(FSKeystore keyStore, CrypterService service, boolean runningGpg) {
      Set<Path> collected = new HashSet<>();
      if (!runningGpg) {
        for (String id : publicKeyIds)
          collected.addAll(keyStore.getPublicKeyFiles(id));
      }
      for (String file : publicKeyFiles)
        collected.add(Paths.get(file));
      if (collected.isEmpty() && !runningGpg)
        throw new RuntimeException("You did not define any public key for encrypting.");
      ArrayList<PublicKeyProxy> keyList = new ArrayList();
      ArrayList<String> idList = new ArrayList();
      for (Path p : collected) {
        PublicKeyProxy publicKey = service.readPublicKey(p);
        keyList.add(publicKey);
        idList.add(publicKey.getPartyId());
      }
      Collections.sort(idList);
      ids = String.join(", ", idList);
      keys = keyList.toArray(new PublicKeyProxy[keyList.size()]);
    }

  }

}
