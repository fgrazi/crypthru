package net.cbsolution.crypthru.crypt;

import lombok.extern.java.Log;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.*;
import org.bouncycastle.util.io.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;


/**
 * The Bouncing-castle PGP implementation
 */
@Log
public class BcPgpService implements CrypterService {

  private static final Pattern EMAIL_ID_PATTERN = Pattern.compile("^.+<(.+)>$");
  public static final int BUFFER_SIZE = 65536;
  public static final int BLOCK_SIZE = 4096;

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  public static class MyPrivateKeyProxy implements PrivateKeyProxy {
    private final PGPSecretKeyRing pgpSecretKeyRing;
    private final String passPhrase;

    private MyPrivateKeyProxy(PGPSecretKeyRing pgpSecretKeyRing, String passPhrase) {
      this.pgpSecretKeyRing = pgpSecretKeyRing;
      this.passPhrase = passPhrase;
    }

    @Override
    public String print() {
      return BcPgpService.print(pgpSecretKeyRing, passPhrase);
    }

    private PGPPrivateKey getPrivateKey() {
      return BcPgpService.getPrivateKey(pgpSecretKeyRing.getSecretKey(), passPhrase);
    }

    Map<Long, PGPSecretKey> keysById() {
      Map<Long, PGPSecretKey> result = new LinkedHashMap<>();
      Iterator<PGPSecretKey> iter = pgpSecretKeyRing.iterator();
      while (iter.hasNext()) {
        PGPSecretKey key = iter.next();
        log.fine("Found private key " + key.getKeyID());
        result.put(key.getKeyID(), key);
      }
      return result;
    }

    @Override
    public void writeTo(Path file) {
      BcPgpService.storeKey(pgpSecretKeyRing.getSecretKey(), file);
    }
  }

  public static class MyPublicKeyProxy implements PublicKeyProxy {

    private final PGPPublicKeyRing pgpPublicKeyRing;

    public MyPublicKeyProxy(PGPPublicKeyRing pgpPublicKeyRing) {

      this.pgpPublicKeyRing = pgpPublicKeyRing;
    }

    @Override
    public String print() {
      return BcPgpService.print(pgpPublicKeyRing);
    }

    @Override
    public void writeTo(Path file) {
      BcPgpService.storeKey(pgpPublicKeyRing.getPublicKey(), file);
    }

    @Override
    public String getPartyId() {
      Iterator<PGPPublicKey> iterator = pgpPublicKeyRing.iterator();
      if (!iterator.hasNext())
        return null;
      Iterator<String> iterator1 = iterator.next().getUserIDs();
      if (!iterator1.hasNext())
        return null;
      String result = iterator1.next();
      Matcher m = EMAIL_ID_PATTERN.matcher(result);
      if (m.matches())
        result = m.group(1);
      return result;
    }
  }

  @Override
  public PublicKeyProxy readPublicKey(Path keyFile) {
    return new MyPublicKeyProxy(loadPublicKeyRing(keyFile));
  }

  @Override
  public PrivateKeyProxy readPrivateKey(Path keyFile, String passPhrase) {
    return new MyPrivateKeyProxy(loadSecretKeyRing(keyFile), passPhrase);
  }

  private static PGPSecretKeyRing loadSecretKeyRing(Path path) {
    try {
      InputStream in = Files.newInputStream(path);
      ArmoredInputStream aIn = new ArmoredInputStream(in);
      final PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(aIn, new JcaKeyFingerprintCalculator());
      PGPSecretKeyRing ring = (PGPSecretKeyRing) pgpObjectFactory.nextObject();
      if (ring == null)
        throw new RuntimeException("No secret key ring in " + path + ". Are you sure this is a valid private key file?");
      return ring;
    } catch (IOException ex) {
      throw new RuntimeException("Error reading " + path, ex);
    }
  }

  @Override
  public KeyPairProxy createKeyPair(String identity, String passPhrase) {
    PGPKeyRingGenerator generator = generateKeyRings(identity, passPhrase);
    return new KeyPairProxy(new MyPrivateKeyProxy(generator.generateSecretKeyRing(), passPhrase),
        new MyPublicKeyProxy(generator.generatePublicKeyRing()));
  }

  private static void storeKey(PGPSecretKey key, Path path) {
    try {
      ArmoredOutputStream out = new ArmoredOutputStream(Files.newOutputStream(path));
      key.encode(out);
      out.close();
    } catch (IOException ex) {
      throw new RuntimeException("Error saving key to " + path);
    }
  }

  private static void storeKey(PGPPublicKey key, Path path) {
    try {
      ArmoredOutputStream out = new ArmoredOutputStream(Files.newOutputStream(path));
      key.encode(out);
      out.close();
    } catch (IOException ex) {
      throw new RuntimeException("Error saving key to " + path);
    }
  }

  private PGPPublicKeyRing loadPublicKeyRing(Path path) {
    try {
      InputStream in = Files.newInputStream(path);
      ArmoredInputStream aIn = new ArmoredInputStream(in);
      final PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(aIn, new JcaKeyFingerprintCalculator());
      PGPPublicKeyRing ring = (PGPPublicKeyRing) pgpObjectFactory.nextObject();
      if (ring == null)
        throw new RuntimeException("No public key ring in " + path + ". Are you sure this is a valid public key file?");
      return ring;
    } catch (IOException ex) {
      throw new RuntimeException("Error reading " + path, ex);
    }
  }

  private static PGPPrivateKey getPrivateKey(PGPSecretKey secretKey, String passphrase) {
    try {
      PBESecretKeyDecryptor decryptorFactory = new BcPBESecretKeyDecryptorBuilder(
          new BcPGPDigestCalculatorProvider()).build(passphrase.toCharArray());
      PGPPrivateKey privateKey = secretKey.extractPrivateKey(decryptorFactory);
      return privateKey;
    } catch (Throwable t) {
      throw new RuntimeException("Failed to extract private key. Most likely it because of incorrect passphrase provided", t);
    }
  }


  @Override
  public void encrypt(Path plainTextFile, Path encryptedFile, PublicKeyProxy... keys) {
    try {
      PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
          new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
              .setWithIntegrityPacket(true)
              .setSecureRandom(new SecureRandom()).setProvider("BC"));

      for (PublicKeyProxy key : keys) {
        PGPPublicKeyRing ring = ((MyPublicKeyProxy) key).pgpPublicKeyRing;
        PGPPublicKey encryptionKey = firstEncryptionKey(ring);
        log.info("Public Key ID: " + getFullId(encryptionKey));
        encGen.addMethod(
            new JcePublicKeyKeyEncryptionMethodGenerator(encryptionKey)
                .setProvider("BC"));
      }

      OutputStream encOut = Files.newOutputStream(encryptedFile);
      InputStream source = Files.newInputStream(plainTextFile);
      try (OutputStream encryptedOut = encGen.open(encOut, new byte[BUFFER_SIZE])) {
        PGPCompressedDataGenerator compressedDataGenerator = new PGPCompressedDataGenerator(PGPCompressedData.ZIP,
            Deflater.BEST_SPEED);
        try (OutputStream compressedOut = compressedDataGenerator.open(encryptedOut, new byte[BUFFER_SIZE])) {
          PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
          try (OutputStream literalOut = literalDataGenerator.open(compressedOut, PGPLiteralData.BINARY,
              plainTextFile.getFileName().toString(), new Date(), new byte[BUFFER_SIZE])) {
            final byte[] buffer = new byte[BLOCK_SIZE];
            int len;
            while ((len = source.read(buffer)) > -1) {
              literalOut.write(buffer, 0, len);
            }
          }
        }
      }


    } catch (IOException | PGPException e) {
      throw new RuntimeException("Error encrypting " + plainTextFile, e);
    }
  }

  private String getFullId(PGPPublicKey key) {
    StringBuilder result = new StringBuilder(Long.toString(key.getKeyID()));
    Iterator<String> iter = key.getUserIDs();
    if (iter.hasNext())
      result.append(" (").append(iter.next()).append(")");
    return result.toString();
  }

  private static String printKeyIds(Collection<Long> ids) {
    StringBuilder result = new StringBuilder("[");
    for (Long id: ids) {
      if (result.length() > 1)
        result.append(", ");
      result.append(id);
    }
    return result.append("]").toString();
  }

  @Override
  public void decrypt(Path encryptedFile, Path plainTextFile, PrivateKeyProxy key) {
    Map<Long, PGPSecretKey> keyMap = ((MyPrivateKeyProxy) key).keysById();
    PGPPrivateKey privateKey = null;
    try {
      InputStream inStream = Files.newInputStream(encryptedFile);
      PGPObjectFactory pgpFact = new JcaPGPObjectFactory(inStream);
      PGPEncryptedDataList encList = (PGPEncryptedDataList) pgpFact.nextObject();

      // find the matching public key encrypted data packet.
      PGPPublicKeyEncryptedData encData = null;
      List<Long> foundId = new ArrayList<>();
      for (PGPEncryptedData pgpEnc : encList) {
        PGPPublicKeyEncryptedData pkEnc = (PGPPublicKeyEncryptedData) pgpEnc;
        foundId.add(pkEnc.getKeyID());
        PGPSecretKey secretKey = keyMap.get(pkEnc.getKeyID());
        if (secretKey != null) {
          privateKey = getPrivateKey(secretKey, ((MyPrivateKeyProxy) key).passPhrase);
          encData = pkEnc;
          break;
        }
      }
      if (encData == null) {
        throw new IllegalStateException("No matching key ID. " + encryptedFile + ": " +
            printKeyIds(foundId) + ", keys: " + printKeyIds(keyMap.keySet()));
      }

      // build decryptor factory
      PublicKeyDataDecryptorFactory dataDecryptorFactory =
          new JcePublicKeyDataDecryptorFactoryBuilder()
              .setProvider("BC")
              .build(privateKey);
      InputStream clear = encData.getDataStream(dataDecryptorFactory);
      byte[] literalData = Streams.readAll(clear);
      clear.close();

      // check data decrypts okay
      if (encData.verify()) {

        // parse out literal data
        PGPObjectFactory litFact = new JcaPGPObjectFactory(literalData);
        Object o = litFact.nextObject();
        OutputStream pOut = Files.newOutputStream(plainTextFile);
        byte[] buf = new byte[8192];
        int length;
        InputStream original;
        if (o instanceof PGPLiteralData) {
          PGPLiteralData litData = (PGPLiteralData) o;
          log.config("Found Literal Data, fileName: " + litData.getFileName() + ", modified: " + litData.getModificationTime());
          original = litData.getInputStream();
        } else if (o instanceof PGPCompressedData) {
          PGPCompressedData compData = (PGPCompressedData) o;
          log.config("Found compressed data, algorithm: " + compData.getAlgorithm() + "");
          pgpFact = new JcaPGPObjectFactory(compData.getDataStream());
          o = (PGPLiteralData)pgpFact.nextObject();
          if (!(o instanceof PGPLiteralData))
            throw new RuntimeException("Unexpected stream in " + encryptedFile + ", unexpected object of type: " + o.getClass().getName());
          PGPLiteralData litData = (PGPLiteralData)o;
          log.config("Compressed data contains literal data, fileName: " + litData.getFileName() + ", modified: " + litData.getModificationTime());
          original = litData.getInputStream();
        } else
          throw new RuntimeException("Unexpected stream in " + encryptedFile + ", unexpected object of type: " + o.getClass().getName());
        while ((length = original.read(buf)) > 0) {
          pOut.write(buf, 0, length);
        }
        pOut.close();
      }
    } catch (PGPException |
        IOException e) {
      throw new RuntimeException("Error decrypting " + encryptedFile, e);
    }
  }

  public static PGPKeyRingGenerator generateDsaRsaKeyRings(String identity, String passphrase) {
    try {
      KeyPair dsaKp = generateDSAKeyPair();
      KeyPair rsaKp = generateRSAKeyPair();
      PGPKeyPair dsaKeyPair = new JcaPGPKeyPair(
          PGPPublicKey.DSA, dsaKp, new Date());
      PGPKeyPair rsaKeyPair = new JcaPGPKeyPair(
          PGPPublicKey.RSA_ENCRYPT, rsaKp, new Date());
      PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder()
          .build().get(HashAlgorithmTags.SHA1);
      PGPKeyRingGenerator keyRingGen =
          new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, dsaKeyPair, identity, sha1Calc,
              null, null,
              new JcaPGPContentSignerBuilder(
                  dsaKeyPair.getPublicKey().getAlgorithm(),
                  HashAlgorithmTags.SHA384),
              new JcePBESecretKeyEncryptorBuilder(
                  PGPEncryptedData.AES_256, sha1Calc)
                  .setProvider("BC").build(passphrase.toCharArray()));
      keyRingGen.addSubKey(rsaKeyPair);
      return keyRingGen;
    } catch (PGPException | GeneralSecurityException e) {
      throw new RuntimeException("Error generating key pair", e);
    }
  }

  public PGPKeyRingGenerator generateKeyRings(String identity, String passphrase) {
    try {
      KeyPair dsaKp = generateRSAKeyPair();
      KeyPair rsaKp = generateRSAKeyPair();
      PGPKeyPair dsaKeyPair = new JcaPGPKeyPair(
          PGPPublicKey.RSA_GENERAL, dsaKp, new Date());
      PGPKeyPair rsaKeyPair = new JcaPGPKeyPair(
          PGPPublicKey.RSA_GENERAL, rsaKp, new Date());
      PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder()
          .build().get(HashAlgorithmTags.SHA1);
      PGPKeyRingGenerator keyRingGen =
          new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, dsaKeyPair, identity, sha1Calc,
              null, null,
              new JcaPGPContentSignerBuilder(
                  dsaKeyPair.getPublicKey().getAlgorithm(),
                  HashAlgorithmTags.SHA384),
              new JcePBESecretKeyEncryptorBuilder(
                  PGPEncryptedData.AES_256, sha1Calc)
                  .setProvider("BC").build(passphrase.toCharArray()));
      keyRingGen.addSubKey(rsaKeyPair);
      return keyRingGen;
    } catch (PGPException | GeneralSecurityException e) {
      throw new RuntimeException("Error generating key pair", e);
    }
  }

  private static KeyPair generateDSAKeyPair()
      throws GeneralSecurityException {
    KeyPairGenerator keyPair = KeyPairGenerator.getInstance("DSA", "BC");
    keyPair.initialize(2048);
    return keyPair.generateKeyPair();
  }

  private static KeyPair generateRSAKeyPair()
      throws GeneralSecurityException {
    KeyPairGenerator keyPair = KeyPairGenerator.getInstance("RSA", "BC");
    keyPair.initialize(
        new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
    return keyPair.generateKeyPair();
  }

  public static String print(PGPPublicKeyRing ring) {
    StringBuilder result = new StringBuilder();
    appendPublicKeys(result, ring.getPublicKeys());
    return result.toString();
  }

  public static String print(PGPSecretKeyRing ring, String passphrase) {
    StringBuilder result = new StringBuilder();
    PGPSecretKey secretKey = ring.getSecretKey();
    PGPPrivateKey privateKey = getPrivateKey(secretKey, passphrase);
    result.append("\n    PRIVATE KEY - id: ").append(privateKey.getKeyID())
        .append(", format: ").append(privateKey.getPrivateKeyDataPacket().getFormat())
        .append(", algorithm: ").append(privateKey.getPublicKeyPacket().getAlgorithm())
        .append(", validDays: ").append(privateKey.getPublicKeyPacket().getValidDays());
    appendPublicKeys(result, ring.getExtraPublicKeys());
    return result.toString();
  }

  private static void appendPublicKeys(StringBuilder sb, Iterator<PGPPublicKey> iter) {
    while (iter.hasNext()) {
      PGPPublicKey key = iter.next();
      sb.append("\n    PUBLIC KEY - id: ").append(key.getKeyID())
          .append(", algorithm: ").append(key.getAlgorithm())
          .append(", master: ").append(key.isMasterKey())
          .append(", encryption: ").append(key.isEncryptionKey())
          .append(", strength: ").append(key.getBitStrength());
    }
  }

  private static PGPPublicKey firstEncryptionKey(PGPPublicKeyRing ring) {
    Iterator<PGPPublicKey> iter = ring.getPublicKeys();
    while (iter.hasNext()) {
      PGPPublicKey key = iter.next();
      if (key.isEncryptionKey())
        return key;
    }
    throw new RuntimeException("No encryption key found in " + ring);
  }


}
