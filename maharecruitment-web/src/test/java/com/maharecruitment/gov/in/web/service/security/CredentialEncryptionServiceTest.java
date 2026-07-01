package com.maharecruitment.gov.in.web.service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

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

    private String encryptWithPublishedPublicKey(String credential) throws Exception {
        PublicKey publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(service.getPublicKey().publicKey())));
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SHA_256);
        byte[] encrypted = cipher.doFinal(credential.getBytes(StandardCharsets.UTF_8));
        return CredentialEncryptionService.ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(encrypted);
    }
}
