package net.cbsolution.crypthru.crypt;

import lombok.Getter;

/**
 * A private + public key pair.
 */
@Getter
public class KeyPairProxy {
  final PrivateKeyProxy privateKeyProxy;
  final PublicKeyProxy publicKeyProxy;

  public KeyPairProxy(PrivateKeyProxy privateKeyProxy, PublicKeyProxy publicKeyProxy) {
    this.privateKeyProxy = privateKeyProxy;
    this.publicKeyProxy = publicKeyProxy;
  }
}
