package net.cbsolution.crypthru;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import net.cbsolution.crypthru.crypt.BcPgpService;
import net.cbsolution.crypthru.crypt.Performance;
import net.cbsolution.crypthru.crypt.PrivateKeyProxy;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Console;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Log
public class Arguments {

  private static final String KEYSTORE_PATH = "~/.crypthru";
  private static final String ASK_ME = "ask-me";

  @Autowired
  FSKeystore fsKeystore;

  private PrivateKeyProxy privateKey;
  private BcPgpService cryptService = new BcPgpService();

  @Parameter(description = "[directive...]")
  private List<String> directives = new ArrayList<>();

  @Parameter(names= "public-id", description = "Public key ID for encrypting")
  private List<String> publicKeys = new ArrayList<>();

  @Parameter(names= "public-key", description = "Public key File for encrypting")
  private List<String> publicKeyFiles = new ArrayList<>();

  @Parameter(names = {"-run", "-r"}, description = "Execute the directives in file(s)")
  private List<String> directiveFile = new ArrayList<>();

  @Parameter(names = "-pass", description = "Passphrase. Use \"" + ASK_ME +
      "\" (unquoted) for interactively ask the passphrase")
  private String passPhrase = "";

  @Parameter(names = {"-private-id", "-prid"} , description = "Your private key ID (if you have many) for decrypting")
  private String privateKeyId;

  @Parameter(names = {"-private-key", "-prk"} , description = "A specific private key file for decrypting")
  private String privateKeyFile;

  @Parameter(names = {"-ks", "-keystore"}, description =
      "Path of the keystore directory (\"keys\" directory parent)")
  private String keyStore = KEYSTORE_PATH;

  @Parameter(names = "-preview", description = "Preview mode: do not execute, only show what would be done")
  private boolean previewMode = false;

  @Parameter(names = "-guide", help = true, description = "Print user's guide on console")
  private boolean guide = false;

  @Parameter(names = "-help", help = true, description = "Show command line help")
  private boolean help = false;

  @Parameter(names = {"-f", "-force"}, description = "Force encryption/decryption independently of file timestamps")
  private boolean force = false;

  @Parameter(names = "-gpg", description = "Execute GPG in background instead of encrypting/decrypting")
  private boolean runGpg = false;

  @Parameter(names = "-watch", description = "Stay watching the directory after scan")
  private boolean watch = false;

  private Performance encryptPerformance = new Performance("Encrypt");

  private Performance decryptPerformance = new Performance("Decrypt");

  public PrivateKeyProxy figurePrivateKey() {
    if (privateKey == null) {
      Path keyFile;
      if (privateKeyFile != null)
        keyFile = Paths.get(privateKeyFile);
      else {
        if (privateKeyId == null)
          privateKeyId = fsKeystore.pickPrivateKey();
        keyFile = fsKeystore.getPrivateKeyFile(privateKeyId);
      }
      if (passPhrase.isEmpty())
        passPhrase = getOrAskPassphrase(privateKeyId);
      else if ("empty".equals(passPhrase))
        passPhrase = "";
      privateKey = cryptService.readPrivateKey(keyFile, passPhrase);
      log.info("Using private key of " + privateKeyId + " loaded from " + keyFile);
    }
    return privateKey;
  }

  public String getOrAskPassphrase(String privateKeyId) {
    if (ASK_ME.equals(passPhrase)) {
      Console console = System.console();
      if (console == null)
        throw new RuntimeException("Your system seems not support system console. Please supply your actual passphrase to -pass.");
      char[] result = console.readPassword("Passphrase for " + privateKeyId + ":");
      passPhrase = new String(result);
    }
    return passPhrase;
  }

}
