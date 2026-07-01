package com.maharecruitment.gov.in.web.service.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CredentialEncryptionService {

    public static final String ENCRYPTED_PREFIX = "ENC:v1:";

    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE_BITS = 2048;
    private static final int MAX_CIPHERTEXT_BYTES = 512;
    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final OAEPParameterSpec OAEP_SHA_256 = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT);

    private final KeyPair keyPair;

    public CredentialEncryptionService() {
        this.keyPair = generateKeyPair();
    }

    public CredentialPublicKey getPublicKey() {
        PublicKey publicKey = keyPair.getPublic();
        return new CredentialPublicKey(
                "RSA-OAEP-256",
                "spki",
                Base64.getEncoder().encodeToString(publicKey.getEncoded()),
                ENCRYPTED_PREFIX);
    }

    public boolean isEncryptedCredential(String value) {
        return StringUtils.hasText(value) && value.startsWith(ENCRYPTED_PREFIX);
    }

    public String decryptCredential(String encryptedCredential) {
        if (!isEncryptedCredential(encryptedCredential)) {
            throw new CredentialDecryptionException("Encrypted credential is required.");
        }

        try {
            byte[] ciphertext = Base64.getDecoder().decode(encryptedCredential.substring(ENCRYPTED_PREFIX.length()));
            if (ciphertext.length == 0 || ciphertext.length > MAX_CIPHERTEXT_BYTES) {
                throw new CredentialDecryptionException("Encrypted credential size is invalid.");
            }

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            PrivateKey privateKey = keyPair.getPrivate();
            cipher.init(Cipher.DECRYPT_MODE, privateKey, OAEP_SHA_256);
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException ex) {
            throw new CredentialDecryptionException("Encrypted credential could not be processed.", ex);
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            generator.initialize(KEY_SIZE_BITS);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to initialize credential encryption keys.", ex);
        }
    }

    public record CredentialPublicKey(
            String algorithm,
            String keyFormat,
            String publicKey,
            String encryptedPrefix) {
    }

    public static class CredentialDecryptionException extends RuntimeException {

        public CredentialDecryptionException(String message) {
            super(message);
        }

        public CredentialDecryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
