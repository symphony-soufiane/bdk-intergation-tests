package com.symphony.bdk.integrationtest.auth.model;

import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Getter
public class RSAKeyModel {

  private PublicKey publicKey = null;
  private PrivateKey privateKey = null;
  private String formattedPublicKey = null;
  private String formattedPrivateKey = null;


  /**
   * Enables us to save on test context keys that are on files inside the Test Resources we're using
   * This was done so we can use an already existing key for debugging.
   *
   * @param filePath   Full filepath of key we want to save on the text context
   * @param rsaKeyType Type of the key (PRIVATE vs PUBLIC) as they're generated differently.
   */
  public void loadKeyFromFile(String filePath, RSAKeyType rsaKeyType) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
    switch (rsaKeyType) {
      case PUBLIC:
        this.publicKey = (PublicKey) getKeyFromFile(RSAKeyType.PUBLIC, filePath);
        this.formattedPublicKey = addBeginAndEndOnRSAKey(RSAKeyType.PUBLIC, publicKey);
        break;
      case PRIVATE:
        this.privateKey = (PrivateKey) getKeyFromFile(RSAKeyType.PRIVATE, filePath);
        this.formattedPrivateKey = addBeginAndEndOnRSAKey(RSAKeyType.PRIVATE, privateKey);
        break;
      default:
        throw new RuntimeException("Invalid value for RSAKeyType!");
    }
  }

  private String addBeginAndEndOnRSAKey(RSAKeyType keyType, Key rsaKey) {
    return addBeginAndEndOnRSAKey(keyType, rsaKey, false);
  }

  private String addBeginAndEndOnRSAKey(RSAKeyType keyType, Key rsaKey, Boolean isInvalid) {
    StringBuilder keyFormatted = new StringBuilder();
    return keyFormatted.append("-----BEGIN " + (keyType.equals(RSAKeyType.PUBLIC) ? "PUBLIC" : "PRIVATE") + " KEY-----")
        .append(System.getProperty("line.separator"))
        .append(Base64.getMimeEncoder().encodeToString(rsaKey.getEncoded()))
        .append(isInvalid ? "invalid" : "")
        .append(System.getProperty("line.separator"))
        .append("-----END " + (keyType.equals(RSAKeyType.PUBLIC) ? "PUBLIC" : "PRIVATE") + " KEY-----")
        .toString();
  }

  private Key getKeyFromFile(RSAKeyType keyType, String filePath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
    String privateKeySTr = IOUtils.toString(inputStream, String.valueOf(StandardCharsets.UTF_8));
    PemObject pem = new PemReader(new StringReader(privateKeySTr)).readPemObject();
    byte[] keyBytes = pem.getContent();
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    switch (keyType) {
      case PUBLIC:
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(keyBytes);
        return keyFactory.generatePublic(pubKeySpec);
      case PRIVATE:
        PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(keyBytes);
        return keyFactory.generatePrivate(ks);
      default:
        throw new RuntimeException("Invalid RSA key type provided for getKeyFromFile method!");
    }
  }
}
