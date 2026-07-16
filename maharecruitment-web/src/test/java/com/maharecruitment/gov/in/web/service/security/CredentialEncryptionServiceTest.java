package com.maharecruitment.gov.in.web.service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.web.service.security.CredentialEncryptionService.CredentialDecryptionException;

class CredentialEncryptionServiceTest {

    private static final OAEPParameterSpec OAEP_SHA_256 = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT);

    private final CredentialEncryptionService service = new CredentialEncryptionService();

    @Test
    void decryptsCredentialEncryptedWithPublishedPublicKey() throws Exception {
        String encrypted = encryptWithPublishedPublicKey("Password@123");

        assertThat(service.decryptCredential(encrypted)).isEqualTo("Password@123");
    }

    @Test
    void rejectsPlaintextCredential() {
        assertThatThrownBy(() -> service.decryptCredential("Password@123"))
                .isInstanceOf(CredentialDecryptionException.class);
    }

    @Test
    void acceptsFreshNonceOnceAndRejectsReplay() throws Exception {
        Clock fixed = Clock.fixed(Instant.parse("2026-07-16T09:00:00Z"), ZoneOffset.UTC);
        CredentialEncryptionService fixedService = new CredentialEncryptionService(fixed);
        String nonce = "abcdefghijklmnopqrstuv";
        String encrypted = encryptSensitive(
                fixedService, "ABCDE2546F", fixed.millis(), nonce, "PAN_UPDATE", "value");
        assertThat(fixedService.decryptSensitivePayload(encrypted, fixedService.getPublicKey().keyId(),
                fixed.millis(), nonce, "PAN_UPDATE")).isEqualTo("ABCDE2546F");
        assertThatThrownBy(() -> fixedService.decryptSensitivePayload(encrypted, fixedService.getPublicKey().keyId(),
                fixed.millis(), nonce, "PAN_UPDATE")).isInstanceOf(CredentialDecryptionException.class);
    }

    @Test
    void rejectsExpiredPayloadAndInvalidKeyId() throws Exception {
        String encrypted = encryptWithPublishedPublicKey(service, "123456782546");
        assertThatThrownBy(() -> service.decryptSensitivePayload(encrypted, "wrong", System.currentTimeMillis(),
                "abcdefghijklmnopqrstuv", "AADHAAR_UPDATE")).isInstanceOf(CredentialDecryptionException.class);
        assertThatThrownBy(() -> service.decryptSensitivePayload(encrypted, service.getPublicKey().keyId(),
                System.currentTimeMillis() - 3600000, "bcdefghijklmnopqrstuvw", "AADHAAR_UPDATE"))
                .isInstanceOf(CredentialDecryptionException.class);
    }

    @Test
    void decryptsMultipleFieldsAfterConsumingReplayMetadataOnce() throws Exception {
        String nonce = "cdefghijklmnopqrstuvwx";
        long timestamp = System.currentTimeMillis();
        String pan = encryptSensitive(
                service, "ABCDE2546F", timestamp, nonce, "DEPARTMENT_REGISTRATION", "panNumber");
        String gst = encryptSensitive(
                service, "27ABCDE1234F1Z5", timestamp, nonce, "DEPARTMENT_REGISTRATION", "gstNumber");

        Map<String, String> decrypted = service.decryptSensitivePayloads(
                Map.of("panNumber", pan, "gstNumber", gst),
                service.getPublicKey().keyId(), timestamp, nonce, "DEPARTMENT_REGISTRATION");

        assertThat(decrypted).containsEntry("panNumber", "ABCDE2546F")
                .containsEntry("gstNumber", "27ABCDE1234F1Z5");
        assertThatThrownBy(() -> service.decryptSensitivePayloads(
                Map.of("panNumber", pan, "gstNumber", gst),
                service.getPublicKey().keyId(), timestamp, nonce, "DEPARTMENT_REGISTRATION"))
                .isInstanceOf(CredentialDecryptionException.class);
    }

    @Test
    void rejectsCapturedCiphertextWhenOuterReplayMetadataIsChanged() throws Exception {
        long timestamp = System.currentTimeMillis();
        String originalNonce = "defghijklmnopqrstuvwxy";
        String encrypted = encryptSensitive(
                service, "ABCDE2546F", timestamp, originalNonce, "PAN_UPDATE", "value");

        assertThatThrownBy(() -> service.decryptSensitivePayload(
                encrypted,
                service.getPublicKey().keyId(),
                timestamp,
                "efghijklmnopqrstuvwxyz",
                "PAN_UPDATE"))
                .isInstanceOf(CredentialDecryptionException.class);
    }

    private String encryptWithPublishedPublicKey(String credential) throws Exception {
        return encryptWithPublishedPublicKey(service, credential);
    }

    private String encryptWithPublishedPublicKey(CredentialEncryptionService target, String credential) throws Exception {
        PublicKey publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(target.getPublicKey().publicKey())));
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SHA_256);
        byte[] encrypted = cipher.doFinal(credential.getBytes(StandardCharsets.UTF_8));
        return CredentialEncryptionService.ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(encrypted);
    }

    private String encryptSensitive(CredentialEncryptionService target, String value, long timestamp,
            String nonce, String purpose, String field) throws Exception {
        String envelope = String.join("\n",
                CredentialEncryptionService.SENSITIVE_PAYLOAD_PREFIX,
                target.getPublicKey().keyId(),
                Long.toString(timestamp),
                nonce,
                purpose,
                field,
                value);
        return encryptWithPublishedPublicKey(target, envelope);
    }
}
