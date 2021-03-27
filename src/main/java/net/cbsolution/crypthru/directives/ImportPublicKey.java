package net.cbsolution.crypthru.directives;

import lombok.extern.java.Log;
import net.cbsolution.crypthru.Arguments;
import net.cbsolution.crypthru.ConfigurationDecoder;
import net.cbsolution.crypthru.Directive;
import net.cbsolution.crypthru.crypt.PublicKeyProxy;

import java.nio.file.Paths;

@Log
public class ImportPublicKey implements Directive {
  private String partyId;
  private String file;

  @Override
  public void configure(ConfigurationDecoder config) {
    file = config.readString("path");
    partyId = config.readString("public-id", "");
  }

  @Override
  public String print() {
    String result = "Import public key from file " + file;
    if (partyId != null)
      result += " as " + partyId;
    return result;
  }

  @Override
  public void execute(Arguments args) {
    PublicKeyProxy publicKey = args.getCryptService().readPublicKey(Paths.get(file));
    if (partyId.isEmpty()) {
      partyId = publicKey.getPartyId();
      if (partyId == null)
        throw new RuntimeException("No user ID found in the key file. Please supply a KeyId");
    }
    args.getFsKeystore().save(partyId, publicKey);
    log.info("Public key " + partyId + " imported");

  }
}
