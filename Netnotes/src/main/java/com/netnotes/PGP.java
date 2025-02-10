package com.netnotes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.io.Streams;
import org.pgpainless.PGPainless;
import org.pgpainless.encryption_signing.EncryptionOptions;
import org.pgpainless.encryption_signing.EncryptionStream;
import org.pgpainless.encryption_signing.ProducerOptions;
import org.pgpainless.util.Passphrase;

public class PGP {
    private PGPSecretKeyRing secretKeys;
    private SecretKey code;

    private String publicKey = "";
    private String clientKeyArmored = "";

    public PGP() throws IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, PGPException {
       
        code = generateStrongAESKey(256);
        byte [] bytes = code.getEncoded();

        String codeString = Hex.toHexString(bytes);


        secretKeys = PGPainless.generateKeyRing().simpleEcKeyRing("ergonotes <ergonotes@ergonotes.io>", Passphrase.fromPassword(codeString));
        PGPPublicKeyRing certificate = PGPainless.extractCertificate(secretKeys);

        publicKey = PGPainless.asciiArmor(certificate);

    }

    public static SecretKey generateStrongAESKey(final int keysize) {
        final KeyGenerator kgen;
        try {
            kgen = KeyGenerator.getInstance("AES");
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("AES key generator should always be available in a Java runtime", e);
        }
        final SecureRandom rng;
        try {
            rng = SecureRandom.getInstanceStrong();
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("No strong secure random available to generate strong AES key", e);
        }
        // already throws IllegalParameterException for wrong key sizes
        kgen.init(keysize, rng);

        return kgen.generateKey();
    }

     public static String getRandomCode(int length) throws NoSuchAlgorithmException {
        String chrs = "0123456789abcdefghijklmnopqrstuvwxyz-_ABCDEFGHIJKLMNOPQRSTUVWXYZ@#$%^&*|;:?";


        SecureRandom secureRandom = SecureRandom.getInstanceStrong();

        
        String randomChrs = secureRandom.ints(length, 0, chrs.length()).mapToObj(i -> chrs.charAt(i))
      .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();



        return randomChrs;
    }

    public String getPublicKey(){
        return this.publicKey;
    }


    public void setClientKey(String key){
        this.clientKeyArmored = key;
    }


    public OutputStream EncryptToClient(String msg){
        
        if(clientKeyArmored == null || clientKeyArmored.length() == 0) return null;

       OutputStream ciphertextOut = null;

       try {
            PGPPublicKeyRing clientCertificate = PGPainless.readKeyRing().publicKeyRing(clientKeyArmored);

            InputStream targetStream = new ByteArrayInputStream(msg.getBytes()); 

            ciphertextOut = new ByteArrayOutputStream();
            EncryptionOptions options = new EncryptionOptions().addRecipient(clientCertificate);
            ProducerOptions producer = ProducerOptions.encrypt(options);

            EncryptionStream encryptionStream = PGPainless.encryptAndOrSign().onOutputStream(ciphertextOut).withOptions(producer);
                  
            Streams.pipeAll(targetStream, encryptionStream);

            encryptionStream.close();

  

        } catch (IOException | PGPException e) {
         
            e.printStackTrace();
        } 

        return ciphertextOut;
    }
}
