package net.cbsolution.crypthru.crypt;

/**
 * A reference to a key used for encrypting data, usually publicly known.
 */
public interface PublicKeyProxy extends KeyProxy {
  String getPartyId();

}
