package net.cbsolution.crypthru.directives;


import lombok.extern.java.Log;
import net.cbsolution.crypthru.Arguments;
import net.cbsolution.crypthru.ConfigurationDecoder;
import net.cbsolution.crypthru.Directive;
import net.cbsolution.crypthru.crypt.KeyPairProxy;
import net.cbsolution.crypthru.util.Dialog;

import java.nio.file.Path;
import java.util.Optional;

@Log
public class CreateKeypair implements Directive {
  private static final int MIN_PASSPHRASE_LEN = 8;
  private static final int MIN_ID_LEN = 3;

  @Override
  public void configure(ConfigurationDecoder config) {
  }

  @Override
  public String print() {
    return "Create Key Pair";
  }

  @Override
  public void execute(Arguments args) {
    Dialog dialog = new Dialog();
    dialog.say("You are going to generate a new key-pair. Please answer the following questions.");
    Optional<String> myId, passPhrase;
    for (; ; ) {
      myId = dialog.ask("What is your ID (email, phone, code name... )");
      if (!myId.isPresent())
        return;
      boolean ok = true;
      if (myId.get().length() < MIN_ID_LEN) {
        dialog.error("Your ID shall have at least {0} characters. Please try again.",
            MIN_ID_LEN);
        ok = false;
      } else if (!myId.get().matches("^[.A-Za-z0-9@_-]+$")) {
        dialog.error("Your ID contains invalid characters.", MIN_ID_LEN);
      }
      if (ok)
        break;
    }
    for (; ; ) {
      passPhrase = dialog.askPassword("Type the passphrase for {0}", myId.get());
      if (!passPhrase.isPresent())
        return;
      if (passPhrase.get().length() < MIN_PASSPHRASE_LEN) {
        dialog.error("Your passphrase shall have at least {0} characters. Please try again.",
            MIN_PASSPHRASE_LEN);
      } else {
        Optional<String> confirm = dialog.askPassword("Confirm (type again) your passphrase");
        if (!confirm.isPresent())
          return;
        if (confirm.get().equals(passPhrase.get()))
          break;
        dialog.error("Your confirmation mismatches. Let's try again...");
      }
    }
    KeyPairProxy keyPair = args.getCryptService().createKeyPair(myId.get(), passPhrase.get());
    Path[] privateAndPublic = args.getFsKeystore().save(myId.get(), keyPair);

    dialog.say("Your public key {3} has been generated into file {0}{1}{2}.\n" +
            "Never mind in protecting or hiding this file. You can publicly transmit to\n" +
        "whoever will send you messages or data.\n", Dialog.ANSI_YELLOW, privateAndPublic[1],
        Dialog.ANSI_GREEN, myId.get());

    dialog.say("Your private {3} key has been generated into file {0}{1}{2}.\n" +
            "{4} .------------------------------------------------------------------------. \n" +
            " | Please keep this file STRICTLY WITH YOU and never transmit to anybody! | \n" +
            " '------------------------------------------------------------------------' {5}\n",
        Dialog.ANSI_YELLOW, privateAndPublic[0], Dialog.ANSI_GREEN, myId.get(), Dialog.ANSI_RED,
        Dialog.ANSI_RESET);
    log.info("New Key pair generated for " + myId);
  }

}
