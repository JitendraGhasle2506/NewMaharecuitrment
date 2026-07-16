package com.maharecruitment.gov.in.web.service.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CredentialEncryptionService {

    public static final String ENCRYPTED_PREFIX = "ENC:v1:";
    public static final String SENSITIVE_PAYLOAD_PREFIX = "SENSITIVE:v1";

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
    private final String keyId;
    private final Clock clock;
    private final ConcurrentHashMap<String, Instant> consumedNonces = new ConcurrentHashMap<>();
    private static final Duration PAYLOAD_TTL = Duration.ofMinutes(5);

    public CredentialEncryptionService() {
        this(Clock.systemUTC());
    }

    CredentialEncryptionService(Clock clock) {
        this.keyPair = generateKeyPair();
        this.keyId = UUID.randomUUID().toString();
        this.clock = clock;
    }

    public CredentialPublicKey getPublicKey() {
        PublicKey publicKey = keyPair.getPublic();
        return new CredentialPublicKey(
                "RSA-OAEP-256",
                "spki",
                Base64.getEncoder().encodeToString(publicKey.getEncoded()),
                ENCRYPTED_PREFIX,
                keyId,
                clock.millis(),
                PAYLOAD_TTL.toMillis());
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

    /** Decrypts a purpose-bound, fresh, one-time browser payload. */
    public String decryptSensitivePayload(String encryptedValue, String submittedKeyId,
            long timestamp, String nonce, String purpose) {
        return decryptSensitivePayloads(
                Map.of("value", encryptedValue), submittedKeyId, timestamp, nonce, purpose).get("value");
    }

    /** Validates freshness/replay once, then decrypts every field in the same request. */
    public Map<String, String> decryptSensitivePayloads(Map<String, String> encryptedValues,
            String submittedKeyId, long timestamp, String nonce, String purpose) {
        if (encryptedValues == null || encryptedValues.isEmpty() || encryptedValues.size() > 3) {
            throw new CredentialDecryptionException("Encrypted request could not be processed.");
        }
        validateRequestMetadata(submittedKeyId, timestamp, nonce, purpose);
        Map<String, String> decrypted = new LinkedHashMap<>();
        encryptedValues.forEach((field, encryptedValue) -> {
            if (field == null || !field.matches("^[a-zA-Z][a-zA-Z0-9]{0,39}$")) {
                throw new CredentialDecryptionException("Encrypted request could not be processed.");
            }
            decrypted.put(field, decryptAndVerifySensitiveEnvelope(
                    encryptedValue, submittedKeyId, timestamp, nonce, purpose, field));
        });
        consumeNonce(nonce, purpose);
        return Map.copyOf(decrypted);
    }

    private void validateRequestMetadata(String submittedKeyId, long timestamp, String nonce, String purpose) {
        Instant now = clock.instant();
        Instant submitted;
        try {
            submitted = Instant.ofEpochMilli(timestamp);
        } catch (RuntimeException ex) {
            throw new CredentialDecryptionException("Encrypted request could not be processed.");
        }
        if (!secureEquals(keyId, submittedKeyId)
                || nonce == null || !nonce.matches("^[A-Za-z0-9_-]{22,128}$")
                || purpose == null || !purpose.matches("^[A-Z_]{3,40}$")
                || submitted.isBefore(now.minus(PAYLOAD_TTL)) || submitted.isAfter(now.plusSeconds(30))) {
            throw new CredentialDecryptionException("Encrypted request could not be processed.");
        }
        consumedNonces.entrySet().removeIf(entry -> entry.getValue().isBefore(now.minus(PAYLOAD_TTL)));
    }

    private String decryptAndVerifySensitiveEnvelope(String encryptedValue, String submittedKeyId,
            long timestamp, String nonce, String purpose, String field) {
        String[] parts = decryptCredential(encryptedValue).split("\\n", -1);
        if (parts.length != 7
                || !SENSITIVE_PAYLOAD_PREFIX.equals(parts[0])
                || !secureEquals(submittedKeyId, parts[1])
                || !Long.toString(timestamp).equals(parts[2])
                || !secureEquals(nonce, parts[3])
                || !secureEquals(purpose, parts[4])
                || !secureEquals(field, parts[5])
                || parts[6].isBlank()) {
            throw new CredentialDecryptionException("Encrypted request could not be processed.");
        }
        return parts[6];
    }

    private void consumeNonce(String nonce, String purpose) {
        if (consumedNonces.putIfAbsent(purpose + ':' + nonce, clock.instant()) != null) {
            throw new CredentialDecryptionException("Encrypted request could not be processed.");
        }
    }

    private boolean secureEquals(String expected, String supplied) {
        return MessageDigest.isEqual(
                String.valueOf(expected).getBytes(StandardCharsets.UTF_8),
                String.valueOf(supplied).getBytes(StandardCharsets.UTF_8));
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
            String encryptedPrefix,
            String keyId,
            long serverTime,
            long maxAgeMillis) {
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
